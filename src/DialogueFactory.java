import java.util.ArrayList;
import java.util.List;

public final class DialogueFactory {
    private DialogueFactory() {}

    public static DialogueSequence createSequence(String[] speakers, String[] blocks) {
        List<DialoguePage> pages = new ArrayList<>();
        int count = Math.min(speakers.length, blocks.length);
        for (int i = 0; i < count; i++) {
            List<String> chunks = splitIntoSentenceChunks(blocks[i], 2);
            for (String chunk : chunks) {
                pages.add(new DialoguePage(speakers[i], chunk));
            }
        }
        return new DialogueSequence(pages);
    }

    private static List<String> splitIntoSentenceChunks(String text, int sentencesPerChunk) {
        List<String> sentences = new ArrayList<>();
        if (text == null) {
            return sentences;
        }
        String normalized = text.replace("\r", " ").replace("\n", " ").trim();
        StringBuilder sentence = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            sentence.append(ch);
            if (ch == '.' || ch == '!' || ch == '?') {
                String s = sentence.toString().trim();
                if (!s.isEmpty()) {
                    sentences.add(s);
                }
                sentence.setLength(0);
            }
        }
        String tail = sentence.toString().trim();
        if (!tail.isEmpty()) {
            sentences.add(tail);
        }

        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i += sentencesPerChunk) {
            StringBuilder chunk = new StringBuilder(sentences.get(i));
            for (int j = 1; j < sentencesPerChunk && i + j < sentences.size(); j++) {
                chunk.append(" ").append(sentences.get(i + j));
            }
            chunks.add(chunk.toString());
        }
        if (chunks.isEmpty()) {
            chunks.add("");
        }
        return chunks;
    }
}
