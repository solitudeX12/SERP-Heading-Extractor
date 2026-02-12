# SERP Heading Extractor

A simple multithreaded Java program that extracts distinct sub-headings from plain text or Markdown files, with heuristics tuned for deep-learning related sections (models, architectures, training, experiments).

## Usage

Compile:

```bash
mkdir -p bin
javac -d bin src/App.java
```

Run (uses bundled sample papers if no directory provided):

```bash
java -cp bin App
```

Run on your directory of papers (text or markdown):

```bash
java -cp bin App /path/to/papers_dir
```

Output

- Writes `distinct_headings.txt` in the working directory with one heading per line.

Heuristics

- Detects Markdown headings, numbered headings, ALL CAPS, and short Title Case lines.
- Filters candidates using keyword matching for deep-learning terms and additional heuristics (stopword ratio, length limits, title-like tokens).

Next steps

- Integrate a lightweight NLP library for part-of-speech filtering to further reduce sentence false positives.
- Add JSON output with source filename and heading location.

## Original VS Code helper

This project also includes the default VS Code Java starter notes in case you want to customize workspace settings.
