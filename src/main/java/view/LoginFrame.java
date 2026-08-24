package view;

import controller.UserController;
import model.User;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private JTextField emailField;
    private JPasswordField passwordField;
    private final UserController userController;

    private final Color ORANGE = new Color(255, 102, 0);
    private final Color BLACK = new Color(18, 18, 18);
    private final Color WHITE = Color.WHITE;
    private final Color LIGHT_GRAY = new Color(245, 245, 245);

    public LoginFrame() {
        userController = new UserController();

        setTitle("Sistema At Promo - Login");
        setSize(900, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(WHITE);

        mainPanel.add(createLeftPanel(), BorderLayout.WEST);
        mainPanel.add(createRightPanel(), BorderLayout.CENTER);

        add(mainPanel);
        setVisible(true);
    }

    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(null);
        panel.setPreferredSize(new Dimension(360, 550));
        panel.setBackground(BLACK);

        JLabel logo = new JLabel("AT");
        logo.setForeground(ORANGE);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 64));
        logo.setBounds(70, 80, 220, 80);
        panel.add(logo);

        JLabel title = new JLabel("PROMO");
        title.setForeground(WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 42));
        title.setBounds(70, 145, 250, 60);
        panel.add(title);

        JLabel subtitle = new JLabel("<html>Gestão interna de promotores,<br>financeiro e solicitações.</html>");
        subtitle.setForeground(new Color(220, 220, 220));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subtitle.setBounds(70, 225, 260, 70);
        panel.add(subtitle);

        JPanel line = new JPanel();
        line.setBackground(ORANGE);
        line.setBounds(70, 320, 120, 5);
        panel.add(line);

        JLabel footer = new JLabel("Sistema corporativo");
        footer.setForeground(new Color(180, 180, 180));
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        footer.setBounds(70, 460, 220, 30);
        panel.add(footer);

        return panel;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(null);
        panel.setBackground(WHITE);

        JLabel title = new JLabel("Acesse sua conta");
        title.setForeground(BLACK);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setBounds(95, 75, 350, 40);
        panel.add(title);

        JLabel subtitle = new JLabel("Entre com seu email e senha para continuar");
        subtitle.setForeground(new Color(100, 100, 100));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setBounds(98, 115, 360, 25);
        panel.add(subtitle);

        JLabel emailLabel = new JLabel("Email");
        emailLabel.setForeground(BLACK);
        emailLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        emailLabel.setBounds(100, 175, 300, 25);
        panel.add(emailLabel);

        emailField = new JTextField();
        emailField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        emailField.setBounds(100, 205, 360, 42);
        emailField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210)),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)
        ));
        panel.add(emailField);

        JLabel passwordLabel = new JLabel("Senha");
        passwordLabel.setForeground(BLACK);
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        passwordLabel.setBounds(100, 270, 300, 25);
        panel.add(passwordLabel);

        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setBounds(100, 300, 360, 42);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210)),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)
        ));
        panel.add(passwordField);

        JButton loginButton = new JButton("Entrar no sistema");
        loginButton.setBounds(100, 375, 360, 45);
        loginButton.setBackground(ORANGE);
        loginButton.setForeground(WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        panel.add(loginButton);

        JLabel hint = new JLabel("Sistema At Promo • RH e Financeiro");
        hint.setForeground(new Color(130, 130, 130));
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        hint.setBounds(180, 445, 260, 25);
        panel.add(hint);

        loginButton.addActionListener(e -> login());

        getRootPane().setDefaultButton(loginButton);

        return panel;
    }

    private void login() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Preencha email e senha.",
                    "Campos obrigatórios",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        User user = userController.login(email, password);

        if (user == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Email ou senha inválidos.",
                    "Erro de login",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        dispose();

        if (user.getJobTittle().equalsIgnoreCase("RH")) {
            new MenuRHFrame(user);
        } else {
            new MenuFinanceiroFrame(user);
        }
    }
}