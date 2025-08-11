import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class ChatBox extends JFrame {
    private final JTextArea chatArea = new JTextArea();
    private final JTextField input = new JTextField();
    private final JButton sendBtn = new JButton("Gửi");
    private final OpenAIClient client;
    private final List<OpenAIClient.Message> history = new ArrayList<>();

    public ChatBox(){
        // Lấy API key từ ENV hoặc VM Option (NetBeans → Properties → Run → VM Options)
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) apiKey = System.getProperty("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            JOptionPane.showMessageDialog(this,
                "Thiếu API key.\nSet biến môi trường OPENAI_API_KEY hoặc VM Option: -DOPENAI_API_KEY=sk-xxxx",
                "Thiếu API Key", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        this.client = new OpenAIClient(apiKey, "gpt-4o-mini");

        setTitle("AI Chat Box");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(640, 520);
        setLocationRelativeTo(null);

        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        JPanel root = new JPanel(new BorderLayout(8,8));
        root.setBorder(new EmptyBorder(10,10,10,10));
        root.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8,8));
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(sendBtn, BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);

        sendBtn.addActionListener(e -> sendMessage());
        input.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    sendMessage();
                }
            }
        });

        appendSystem("Nhập câu hỏi và Enter để gửi.");
    }

    private void sendMessage() {
        String text = input.getText().trim();
        if (text.isEmpty()) return;
        input.setText("");
        appendUser(text);

        history.add(new OpenAIClient.Message("user", text));
        sendBtn.setEnabled(false);

        new SwingWorker<String, Void>(){
            @Override protected String doInBackground() throws Exception {
                return client.chat(history);
            }
            @Override protected void done() {
                sendBtn.setEnabled(true);
                try {
                    String reply = get();
                    history.add(new OpenAIClient.Message("assistant", reply));
                    appendAI(reply);
                } catch (Exception ex) {
                    appendSystem("Lỗi: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void appendUser(String msg) { chatArea.append("Bạn: " + msg + "\n"); }
    private void appendAI(String msg)   { chatArea.append("AI: " + msg + "\n\n"); }
    private void appendSystem(String m) { chatArea.append("[Hệ thống] " + m + "\n"); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatBox().setVisible(true));
    }
}
