import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TestNLP {
    // Vietnamese stop words for filtering
    private static final Set<String> VI_STOP_WORDS = Set.of(
            "tôi", "tao", "mình", "bọn", "chúng", "các", "những", "của", "và", "hoặc",
            "nhưng", "mà", "thì", "là", "được", "để", "cho", "với", "trong", "ngoài",
            "trên", "dưới", "về", "từ", "đến", "có", "không", "cũng", "đã", "sẽ",
            "đang", "rồi", "nên", "vì", "bởi", "do", "nếu", "hay", "này",
            "đó", "kia", "nào", "gì", "sao", "thế", "hôm", "nay", "ngày", "mai",
            "muốn", "cần", "phải", "biết", "hiểu", "tìm", "làm", "viết", "đọc",
            "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín", "mười",
            "rất", "quá", "lắm", "hơn", "nhất", "ít", "nhiều", "bao"
    );

    // English stop words for filtering
    private static final Set<String> EN_STOP_WORDS = Set.of(
            "i", "me", "my", "myself", "we", "our", "you", "your", "he", "she", "it",
            "they", "them", "what", "which", "who", "this", "that", "these", "those",
            "am", "is", "are", "was", "were", "be", "been", "being", "have", "has",
            "had", "does", "did", "will", "would", "shall", "should", "can",
            "could", "may", "might", "must", "a", "an", "the", "and", "but", "or",
            "if", "while", "of", "at", "by", "for", "with", "about", "between",
            "to", "from", "in", "on", "into", "through", "during", "before", "after",
            "above", "below", "up", "down", "out", "off", "over", "under", "again",
            "then", "once", "here", "there", "when", "where", "why", "how", "all",
            "each", "every", "both", "few", "more", "most", "some", "such", "no",
            "not", "only", "own", "same", "so", "than", "too", "very", "just",
            "want", "need", "today", "research", "study", "explore", "find", "look",
            "make", "work", "write", "read", "know", "understand", "learn"
    );

    private static final Map<String, String> VI_EN_MAPPING = new LinkedHashMap<>();
    static {
        VI_EN_MAPPING.put("trí tuệ nhân tạo", "artificial intelligence");
        VI_EN_MAPPING.put("học máy", "machine learning");
        VI_EN_MAPPING.put("học sâu", "deep learning");
        VI_EN_MAPPING.put("xử lý ngôn ngữ tự nhiên", "natural language processing");
        VI_EN_MAPPING.put("thị giác máy tính", "computer vision");
        VI_EN_MAPPING.put("đạo đức ai", "AI ethics");
        VI_EN_MAPPING.put("đạo đức trí tuệ nhân tạo", "AI ethics");
        VI_EN_MAPPING.put("chuỗi khối", "blockchain");
        VI_EN_MAPPING.put("internet vạn vật", "internet of things");
        VI_EN_MAPPING.put("dữ liệu lớn", "big data");
        VI_EN_MAPPING.put("an ninh mạng", "cybersecurity");
        VI_EN_MAPPING.put("năng lượng tái tạo", "renewable energy");
        VI_EN_MAPPING.put("biến đổi khí hậu", "climate change");
        VI_EN_MAPPING.put("khoa học dữ liệu", "data science");
        VI_EN_MAPPING.put("robot", "robotics");
        VI_EN_MAPPING.put("điện toán đám mây", "cloud computing");
        VI_EN_MAPPING.put("thực tế ảo", "virtual reality");
        VI_EN_MAPPING.put("thực tế tăng cường", "augmented reality");
        VI_EN_MAPPING.put("công nghệ sinh học", "biotechnology");
        VI_EN_MAPPING.put("y học", "medicine");
        VI_EN_MAPPING.put("giáo dục", "education");
        VI_EN_MAPPING.put("kinh tế", "economics");
        VI_EN_MAPPING.put("tài chính", "finance");
        VI_EN_MAPPING.put("tâm lý học", "psychology");
        VI_EN_MAPPING.put("xã hội học", "sociology");
        VI_EN_MAPPING.put("mô hình ngôn ngữ lớn", "large language model");
    }

    public static void main(String[] args) {
        String input = "tôi muốn nghiên cứu về chủ đề đạo đức của AI";
        String normalized = input.toLowerCase().trim();
        List<String> keywords = new ArrayList<>();

        // Step 1: Check Vietnamese → English mappings (longest match first)
        String remaining = normalized;
        for (Map.Entry<String, String> entry : VI_EN_MAPPING.entrySet()) {
            if (remaining.contains(entry.getKey())) {
                keywords.add(entry.getValue());
                remaining = remaining.replace(entry.getKey(), " ");
            }
        }

        System.out.println("After Step 1: " + remaining);

        // Step 2: Extract remaining meaningful words (filter stop words)
        String[] words = remaining.split("[\\s,;.!?\"'()\\[\\]{}]+");
        List<String> meaningfulWords = new ArrayList<>();
        for (String word : words) {
            word = word.trim().replaceAll("[^a-zA-Zàáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]", "");
            if (word.length() > 2
                    && !VI_STOP_WORDS.contains(word)
                    && !EN_STOP_WORDS.contains(word)) {
                meaningfulWords.add(word);
            }
        }
        
        System.out.println("Meaningful words: " + meaningfulWords);
    }
}
