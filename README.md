# SERP Heading Extractor

A simple multithreaded Java program that extracts distinct sub-headings from plain text or Markdown files, with heuristics tuned for deep-learning related sections (models, architectures, training, experiments).

Search Engine Results Pages (SERP) are the pages displayed by search engines in response to a query by a user. Each result displayed normally includes a title, a link that points to the actual page on the Web, and a short description showing where the keywords have matched content within the page for organic results. The SERP are ranked based on relevance for organic results. Considering the semantic search of users’ query gives more accurate SERP. More importantly, summarizing the relevant content of SERP for users instead of the displayed titles and links will be more useful to users.
This designs and implement a multithreaded program for returning distinct sub-headings journal papers (on deep learning models).

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

