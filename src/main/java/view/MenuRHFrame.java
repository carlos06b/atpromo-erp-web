package view;

import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MenuRHFrame extends JFrame {

    private final Color ORANGE = new Color(255, 102, 0);
    private final Color BLACK = new Color(18, 18, 18);
    private final Color WHITE = Color.WHITE;
    private final Color LIGHT_GRAY = new Color(245, 245, 245);
    private final Color DARK_GRAY = new Color(55, 55, 55);
    private final Color BORDER = new Color(225, 225, 225);
    private final Color SOFT_ORANGE = new Color(255, 244, 235);

    public MenuRHFrame(User user) {
        setTitle("Sistema At Promo - RH");
        setSize(1200, 760);
        setMinimumSize(new Dimension(1100, 700));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(LIGHT_GRAY);

        mainPanel.add(createSidebar(user), BorderLayout.WEST);
        mainPanel.add(createContentPanel(user), BorderLayout.CENTER);

        add(mainPanel);
        setVisible(true);
    }

    private JPanel createSidebar(User user) {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(285, 0));
        sidebar.setBackground(BLACK);

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(new EmptyBorder(30, 28, 20, 28));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brand.setOpaque(false);
        brand.setMaximumSize(new Dimension(230, 58));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel logoAt = new JLabel("AT");
        logoAt.setForeground(ORANGE);
        logoAt.setFont(new Font("Segoe UI", Font.BOLD, 42));
        brand.add(logoAt);

        JLabel logoPromo = new JLabel(" PROMO");
        logoPromo.setForeground(WHITE);
        logoPromo.setFont(new Font("Segoe UI", Font.BOLD, 26));
        brand.add(logoPromo);

        JPanel orangeLine = new JPanel();
        orangeLine.setBackground(ORANGE);
        orangeLine.setMaximumSize(new Dimension(210, 4));
        orangeLine.setPreferredSize(new Dimension(210, 4));
        orangeLine.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel module = new JLabel("Módulo RH");
        module.setForeground(WHITE);
        module.setFont(new Font("Segoe UI", Font.BOLD, 20));
        module.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Gestão de pessoas");
        subtitle.setForeground(new Color(170, 170, 170));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        top.add(brand);
        top.add(Box.createVerticalStrut(12));
        top.add(orangeLine);
        top.add(Box.createVerticalStrut(32));
        top.add(module);
        top.add(Box.createVerticalStrut(4));
        top.add(subtitle);
        top.add(Box.createVerticalStrut(28));
        top.add(createUserBox(user));
        top.add(Box.createVerticalStrut(28));

        top.add(createSidebarButton("Painel RH", true, null));
        top.add(Box.createVerticalStrut(10));
        top.add(createSidebarButton("Promotores", false, () -> new PromoterFrame()));
        top.add(Box.createVerticalStrut(10));
        top.add(createSidebarButton("Solicitações", false, () -> new RequestFrame(user)));
        top.add(Box.createVerticalStrut(10));
        top.add(createSidebarButton("Folha de Pagamento", false, () -> new PayrollFrame()));

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBorder(new EmptyBorder(0, 28, 30, 28));

        JLabel footer = new JLabel("Sistema interno AT Promo");
        footer.setForeground(new Color(145, 145, 145));
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton logoutButton = createLogoutButton();

        bottom.add(footer);
        bottom.add(Box.createVerticalStrut(14));
        bottom.add(logoutButton);

        sidebar.add(top, BorderLayout.CENTER);
        sidebar.add(bottom, BorderLayout.SOUTH);

        return sidebar;
    }

    private JPanel createUserBox(User user) {
        JPanel box = new JPanel(new BorderLayout(0, 5));
        box.setBackground(new Color(30, 30, 30));
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(48, 48, 48)),
                new EmptyBorder(14, 14, 14, 14)
        ));
        box.setMaximumSize(new Dimension(230, 86));
        box.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel name = new JLabel(user.getName());
        name.setForeground(WHITE);
        name.setFont(new Font("Segoe UI", Font.BOLD, 15));

        JLabel role = new JLabel("Cargo: " + user.getJobTittle());
        role.setForeground(new Color(180, 180, 180));
        role.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        box.add(name, BorderLayout.NORTH);
        box.add(role, BorderLayout.CENTER);

        return box;
    }

    private JPanel createContentPanel(User user) {
        JPanel panel = new JPanel(new BorderLayout(0, 24));
        panel.setBackground(LIGHT_GRAY);
        panel.setBorder(new EmptyBorder(34, 42, 34, 42));

        panel.add(createTopHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 24));
        center.setOpaque(false);

        center.add(createOverviewPanel(), BorderLayout.NORTH);
        center.add(createModulesPanel(user), BorderLayout.CENTER);

        panel.add(center, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createTopHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titleArea = new JPanel();
        titleArea.setOpaque(false);
        titleArea.setLayout(new BoxLayout(titleArea, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Painel de Recursos Humanos");
        title.setForeground(BLACK);
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));

        JLabel subtitle = new JLabel("Controle operacional de promotores, solicitações e conferência da folha.");
        subtitle.setForeground(DARK_GRAY);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));

        titleArea.add(title);
        titleArea.add(Box.createVerticalStrut(6));
        titleArea.add(subtitle);

        JLabel badge = new JLabel("RH");
        badge.setOpaque(true);
        badge.setBackground(ORANGE);
        badge.setForeground(WHITE);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 16));
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setPreferredSize(new Dimension(72, 38));

        header.add(titleArea, BorderLayout.WEST);
        header.add(badge, BorderLayout.EAST);

        return header;
    }

    private JPanel createOverviewPanel() {
        JPanel overview = new JPanel(new GridLayout(1, 3, 16, 0));
        overview.setOpaque(false);
        overview.setPreferredSize(new Dimension(0, 112));

        overview.add(createInfoCard("Cadastros", "Promotores ativos e dados profissionais", ORANGE));
        overview.add(createInfoCard("Solicitações", "Pedidos enviados ao financeiro", BLACK));
        overview.add(createInfoCard("Folha", "Conferência mensal de pagamento", new Color(80, 80, 80)));

        return overview;
    }

    private JPanel createInfoCard(String title, String description, Color accent) {
        JPanel card = new JPanel(new BorderLayout(14, 0));
        card.setBackground(WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(18, 18, 18, 18)
        ));

        JPanel bar = new JPanel();
        bar.setBackground(accent);
        bar.setPreferredSize(new Dimension(5, 1));
        card.add(bar, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(BLACK);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JLabel descriptionLabel = new JLabel("<html>" + description + "</html>");
        descriptionLabel.setForeground(DARK_GRAY);
        descriptionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        text.add(titleLabel);
        text.add(Box.createVerticalStrut(8));
        text.add(descriptionLabel);

        card.add(text, BorderLayout.CENTER);

        return card;
    }

    private JPanel createModulesPanel(User user) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 14));
        wrapper.setOpaque(false);

        JLabel sectionTitle = new JLabel("Módulos disponíveis");
        sectionTitle.setForeground(BLACK);
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JPanel grid = new JPanel(new GridLayout(2, 2, 18, 18));
        grid.setOpaque(false);

        grid.add(createActionCard(
                "Promotores",
                "Cadastre, edite, consulte, ative e inative promotores. Use este módulo como base principal do RH.",
                "Abrir promotores",
                ORANGE,
                () -> new PromoterFrame()
        ));

        grid.add(createActionCard(
                "Solicitações",
                "Registre pedidos de ajuda de custo, bonificação, desconto, EPI, ASO e acompanhe o retorno financeiro.",
                "Abrir solicitações",
                BLACK,
                () -> new RequestFrame(user)
        ));

        grid.add(createActionCard(
                "Folha de Pagamento",
                "Conferência de valores do período, promotores MEI, CLT e feristas, com apoio para pagamento.",
                "Abrir folha",
                new Color(70, 70, 70),
                () -> new PayrollFrame(user)
        ));

        grid.add(createRoutineCard());

        wrapper.add(sectionTitle, BorderLayout.NORTH);
        wrapper.add(grid, BorderLayout.CENTER);

        return wrapper;
    }

    private JPanel createActionCard(String title, String description, String buttonText, Color accent, Runnable action) {
        JPanel card = new JPanel(new BorderLayout(0, 16));
        card.setBackground(WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(22, 22, 22, 22)
        ));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);

        JPanel mark = new JPanel();
        mark.setBackground(accent);
        mark.setPreferredSize(new Dimension(7, 34));
        top.add(mark, BorderLayout.WEST);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(BLACK);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 21));
        top.add(titleLabel, BorderLayout.CENTER);

        JTextArea descriptionArea = new JTextArea(description);
        descriptionArea.setEditable(false);
        descriptionArea.setOpaque(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setForeground(DARK_GRAY);
        descriptionArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        descriptionArea.setFocusable(false);

        JButton button = createCardButton(buttonText, action);

        card.add(top, BorderLayout.NORTH);
        card.add(descriptionArea, BorderLayout.CENTER);
        card.add(button, BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(SOFT_ORANGE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(WHITE);
            }
        });

        return card;
    }

    private JPanel createRoutineCard() {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(22, 22, 22, 22)
        ));

        JLabel title = new JLabel("Rotina recomendada");
        title.setForeground(BLACK);
        title.setFont(new Font("Segoe UI", Font.BOLD, 21));

        JTextArea text = new JTextArea(
                "1. Mantenha os dados dos promotores atualizados.\n" +
                        "2. Registre solicitações com descrição clara.\n" +
                        "3. Confira a folha antes do fechamento mensal.\n" +
                        "4. Gere arquivos de pagamento somente após validação."
        );
        text.setEditable(false);
        text.setOpaque(false);
        text.setForeground(DARK_GRAY);
        text.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        text.setFocusable(false);

        card.add(title, BorderLayout.NORTH);
        card.add(text, BorderLayout.CENTER);

        return card;
    }

    private JButton createSidebarButton(String text, boolean active, Runnable action) {
        JButton button = new JButton(text);
        button.setMaximumSize(new Dimension(230, 42));
        button.setPreferredSize(new Dimension(230, 42));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (active) {
            button.setBackground(ORANGE);
            button.setForeground(WHITE);
        } else {
            button.setBackground(new Color(28, 28, 28));
            button.setForeground(new Color(220, 220, 220));
        }

        if (action != null) {
            button.addActionListener(e -> action.run());
        }

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!active) {
                    button.setBackground(new Color(42, 42, 42));
                    button.setForeground(WHITE);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!active) {
                    button.setBackground(new Color(28, 28, 28));
                    button.setForeground(new Color(220, 220, 220));
                }
            }
        });

        return button;
    }

    private JButton createCardButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setBackground(BLACK);
        button.setForeground(WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setPreferredSize(new Dimension(150, 38));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> action.run());

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(ORANGE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(BLACK);
            }
        });

        return button;
    }

    private JButton createLogoutButton() {
        JButton button = new JButton("Sair do sistema");
        button.setMaximumSize(new Dimension(230, 42));
        button.setPreferredSize(new Dimension(230, 42));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setBackground(WHITE);
        button.setForeground(BLACK);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addActionListener(e -> {
            dispose();
            new LoginFrame();
        });

        return button;
    }
}