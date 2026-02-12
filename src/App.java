import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class App {
    // Keywords that indicate headings relevant to deep learning models
    private static final String[] KEYWORDS = new String[]{
            "deep", "neural", "network", "model", "models", "architecture",
            "cnn", "convolutional", "rnn", "lstm", "gru", "transformer", "attention",
            "training", "inference", "evaluation", "experiments"
    };

    public static void main(String[] args) throws Exception {
        Path papersDir;
        if (args.length > 0) {
            papersDir = Paths.get(args[0]);
        } else {
            // If no directory provided, create a small sample set and run on it
            papersDir = Paths.get("papers_sample");
            createSamplePapers(papersDir);
        }

        if (!Files.exists(papersDir) || !Files.isDirectory(papersDir)) {
            System.err.println("Papers directory does not exist: " + papersDir.toAbsolutePath());
            System.exit(1);
        }

        Set<String> distinctHeadings = extractDistinctHeadings(papersDir);

        System.out.println("Distinct deep-learning-related sub-headings found:");
        for (String h : distinctHeadings) {
            System.out.println("- " + h);
        }

        // write to output file
        Path out = Paths.get("distinct_headings.txt");
        try (BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            for (String h : distinctHeadings) {
                w.write(h);
                w.newLine();
            }
        }

        System.out.println("Wrote " + distinctHeadings.size() + " headings to " + out.toAbsolutePath());
    }

    private static Set<String> extractDistinctHeadings(Path dir) throws Exception {
        File[] files = dir.toFile().listFiles((d, name) -> name.toLowerCase().endsWith(".txt") || name.toLowerCase().endsWith(".md"));
        if (files == null) files = new File[0];

        ExecutorService ex = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));
        List<Callable<Set<String>>> tasks = new ArrayList<>();

        for (File f : files) {
            final Path p = f.toPath();
            tasks.add(() -> extractHeadingsFromFile(p));
        }

        List<Future<Set<String>>> results = ex.invokeAll(tasks);
        ex.shutdown();

        Set<String> combined = new ConcurrentSkipListSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Future<Set<String>> fut : results) {
            try {
                Set<String> r = fut.get();
                combined.addAll(r);
            } catch (Exception e) {
                // continue on errors per-file
            }
        }

        return combined;
    }

    private static Set<String> extractHeadingsFromFile(Path file) {
        Set<String> headings = new HashSet<>();

        Pattern numbered = Pattern.compile("^\\s*(\\d+(?:\\.\\d+)*)\\s+(.{3,200})$");
        Pattern mdHeading = Pattern.compile("^#{1,6}\\s+(.+)$");
        Pattern allCaps = Pattern.compile("^[A-Z0-9][A-Z0-9\\s,:\\-()]{2,150}$");

        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                String candidate = null;

                Matcher m1 = mdHeading.matcher(line);
                if (m1.find()) {
                    candidate = m1.group(1).trim();
                } else {
                    Matcher m2 = numbered.matcher(line);
                    if (m2.find()) {
                        candidate = m2.group(2).trim();
                    } else {
                        // treat a line in ALL CAPS as heading
                        Matcher m3 = allCaps.matcher(line);
                        if (m3.find()) candidate = line;
                        else {
                            // heuristic: short lines (<=100 chars) with Title Case could be sub-headings
                            if (line.length() <= 100 && looksLikeTitleCase(line)) {
                                candidate = line;
                            }
                        }
                    }
                }

                if (candidate != null && containsDeepKeyword(candidate)) {
                    // normalize whitespace and strip trailing punctuation/ellipses
                    String normalized = candidate.replaceAll("\\s+", " ").trim();
                    normalized = normalized.replaceAll("[\\.]{2,}$", "");
                    normalized = normalized.replaceAll("[\\.:;\\-\\s]+$", "").trim();

                    // reject if too long or too short
                    String[] words = normalized.split("\\s+");
                    if (words.length < 1 || words.length > 12) continue;

                    // stopword ratio heuristic: headings tend to have fewer stopwords
                    int stopCount = 0;
                    for (String w : words) {
                        if (STOPWORDS.contains(w.toLowerCase())) stopCount++;
                    }
                    double stopRatio = (double) stopCount / (double) words.length;
                    if (stopRatio > 0.5) continue;

                    // require at least one 'title-like' token (starts with uppercase or is ALL CAPS)
                    int titleLike = 0;
                    for (String w : words) {
                        if (w.length() > 1 && Character.isUpperCase(w.charAt(0))) titleLike++;
                        else if (w.equals(w.toUpperCase()) && w.length() > 1) titleLike++;
                    }
                    if (titleLike < 1) continue;

                    headings.add(normalized);
                }
            }
        } catch (IOException e) {
            // ignore file read errors
        }

        return headings;
    }

    private static boolean containsDeepKeyword(String text) {
        String low = text.toLowerCase();
        for (String k : KEYWORDS) if (low.contains(k)) return true;
        return false;
    }

    private static final Set<String> STOPWORDS = new HashSet<>();
    static {
        String[] s = new String[]{"a","an","the","and","or","of","in","on","for","with","to","by","from","that","this","we","is","are","was","were","be","using","based","our","as","into","over","between","at"};
        for (String w : s) STOPWORDS.add(w);
    }

    private static boolean looksLikeTitleCase(String s) {
        // simple heuristic: at least two words start with uppercase letter
        String[] parts = s.split("\\s+");
        int count = 0;
        for (String p : parts) {
            if (p.length() > 0 && Character.isUpperCase(p.charAt(0))) count++;
            if (count >= 2) return true;
        }
        return false;
    }

    private static void createSamplePapers(Path dir) throws IOException {
        if (!Files.exists(dir)) Files.createDirectories(dir);

        String paper1 = "1 Introduction\nDeep learning models have shown...\n2 Related Work\nPrevious CNN architectures...\n2.1 Convolutional Neural Networks\nDetails about CNNs...\n3 Experiments\nTraining and Evaluation\n";
        String paper2 = "# Abstract\nThis paper describes a transformer-based model...\n# 1. Introduction\nTransformers changed NLP...\n## Model Architecture\nWe propose a novel transformer architecture...\n## Experimental Setup\n";
        String paper3 = "INTRODUCTION\nDeep Neural Networks for image recognition...\nMODEL AND ARCHITECTURE\nWe use a ResNet-inspired model...\nTRAINING PROCEDURE\n";

        Files.write(dir.resolve("paper1.txt"), paper1.getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve("paper2.md"), paper2.getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve("paper3.txt"), paper3.getBytes(StandardCharsets.UTF_8));
    }
}
