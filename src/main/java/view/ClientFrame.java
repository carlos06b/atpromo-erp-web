package view;

import controller.ClientController;
import model.Client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;

public class ClientFrame extends JFrame {

    private final ClientController clientController = new ClientController();

    private final Color ORANGE = new Color(255, 102, 0);
    private final Color BLACK = new Color(18, 18, 18);
    private final Color WHITE = Color.WHITE;
    private final Color LIGHT_GRAY = new Color(245, 245, 245);
    private final Color RED = new Color(180, 40, 40);
    private final Color GREEN = new Color(35, 140, 80);

    private JTextField txtSearch;
    private JComboBox<String> cbStatusFilter;
    private JComboBox<String> cbCompanyFilter;

    private JTable table;
    private DefaultTableModel tableModel;

    public ClientFrame() {
        setTitle("Sistema At Promo - Clientes / Indústrias");
        setSize(1000, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel mainPanel = new JPanel(null);
        mainPanel.setBackground(LIGHT_GRAY);
        add(mainPanel);

        createHeader(mainPanel);
        createFilterPanel(mainPanel);
        createTable(mainPanel);
        createActionButtons(mainPanel);

        loadClients();

        setVisible(true);
    }

    private void createHeader(JPanel panel) {
        JPanel header = new JPanel(null);
        header.setBackground(BLACK);
        header.setBounds(0, 0, 980, 95);
        panel.add(header);

        JLabel title = new JLabel("Clientes / Indústrias");
        title.setForeground(WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setBounds(30, 18, 400, 35);
        header.add(title);

        JLabel subtitle = new JLabel("Cadastro, filtros e controle de vínculo AT / TEJO");
        subtitle.setForeground(new Color(210, 210, 210));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        subtitle.setBounds(32, 55, 600, 25);
        header.add(subtitle);

        JPanel orangeLine = new JPanel();
        orangeLine.setBackground(ORANGE);
        orangeLine.setBounds(30, 85, 900, 4);
        header.add(orangeLine);
    }

    private void createFilterPanel(JPanel panel) {
        JPanel filterPanel = new JPanel(null);
        filterPanel.setBackground(WHITE);
        filterPanel.setBounds(30, 120, 910, 95);
        filterPanel.setBorder(BorderFactory.createLineBorder(new Color(225, 225, 225)));
        panel.add(filterPanel);

        JLabel lblSearch = new JLabel("Buscar por nome");
        lblSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSearch.setBounds(20, 12, 150, 20);
        filterPanel.add(lblSearch);

        txtSearch = new JTextField();
        txtSearch.setBounds(20, 38, 260, 32);
        filterPanel.add(txtSearch);

        JLabel lblStatus = new JLabel("Status");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblStatus.setBounds(300, 12, 100, 20);
        filterPanel.add(lblStatus);

        cbStatusFilter = new JComboBox<>(new String[]{"Todos", "Ativos", "Inativos"});
        cbStatusFilter.setBounds(300, 38, 130, 32);
        filterPanel.add(cbStatusFilter);

        JLabel lblCompany = new JLabel("Vínculo");
        lblCompany.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblCompany.setBounds(450, 12, 100, 20);
        filterPanel.add(lblCompany);

        cbCompanyFilter = new JComboBox<>(new String[]{"Todos", "AT", "TEJO"});
        cbCompanyFilter.setBounds(450, 38, 130, 32);
        filterPanel.add(cbCompanyFilter);

        JButton btnFilter = createDarkButton("Filtrar");
        btnFilter.setBounds(610, 38, 100, 32);
        btnFilter.addActionListener(e -> applyFilters());
        filterPanel.add(btnFilter);

        JButton btnRefresh = createSecondaryButton("Atualizar");
        btnRefresh.setBounds(720, 38, 100, 32);
        btnRefresh.addActionListener(e -> resetFilters());
        filterPanel.add(btnRefresh);

        JButton btnRegister = createPrimaryButton("Novo");
        btnRegister.setBounds(830, 38, 70, 32);
        btnRegister.addActionListener(e -> openRegisterDialog());
        filterPanel.add(btnRegister);
    }

    private void createTable(JPanel panel) {
        tableModel = new DefaultTableModel(
                new Object[]{"ID", "Nome Fantasia", "CNPJ", "Telefone", "Email", "Vínculo", "Status"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(new Color(230, 230, 230));

        JTableHeader header = table.getTableHeader();
        header.setBackground(BLACK);
        header.setForeground(WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBounds(30, 235, 910, 270);
        panel.add(scrollPane);

        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);

        table.getColumnModel().getColumn(1).setPreferredWidth(240);
        table.getColumnModel().getColumn(2).setPreferredWidth(135);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(190);
        table.getColumnModel().getColumn(5).setPreferredWidth(80);
        table.getColumnModel().getColumn(6).setPreferredWidth(80);
    }

    private void createActionButtons(JPanel panel) {
        JButton btnEdit = createSecondaryButton("Editar");
        btnEdit.setBounds(225, 525, 100, 36);
        btnEdit.addActionListener(e -> openEditDialog());
        panel.add(btnEdit);

        JButton btnDetails = createSecondaryButton("Ver Detalhes");
        btnDetails.setBounds(335, 525, 125, 36);
        btnDetails.addActionListener(e -> showClientDetails());
        panel.add(btnDetails);

        JButton btnExcel = createPrimaryButton("Exportar Excel");
        btnExcel.setBounds(470, 525, 150, 36);
        btnExcel.addActionListener(e -> exportExcel());
        panel.add(btnExcel);

        JButton btnActivate = createPrimaryButton("Ativar");
        btnActivate.setBounds(630, 525, 100, 36);
        btnActivate.addActionListener(e -> activateClient());
        panel.add(btnActivate);

        JButton btnDeactivate = createDangerButton("Inativar");
        btnDeactivate.setBounds(745, 525, 100, 36);
        btnDeactivate.addActionListener(e -> deactivateClient());
        panel.add(btnDeactivate);

        JButton btnClose = createDarkButton("Fechar");
        btnClose.setBounds(860, 525, 110, 36);
        btnClose.addActionListener(e -> dispose());
        panel.add(btnClose);
    }

    private void openRegisterDialog() {
        JTextField txtCorporateName = new JTextField();
        JTextField txtFantasyName = new JTextField();
        JTextField txtCnpj = new JTextField();
        JTextField txtPhone = new JTextField();
        JTextField txtEmail = new JTextField();
        JComboBox<String> cbCompanyLink = new JComboBox<>(new String[]{"AT", "TEJO"});

        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.add(new JLabel("Razão social:"));
        panel.add(txtCorporateName);
        panel.add(new JLabel("Nome fantasia:"));
        panel.add(txtFantasyName);
        panel.add(new JLabel("CNPJ:"));
        panel.add(txtCnpj);
        panel.add(new JLabel("Telefone:"));
        panel.add(txtPhone);
        panel.add(new JLabel("Email:"));
        panel.add(txtEmail);
        panel.add(new JLabel("Vínculo:"));
        panel.add(cbCompanyLink);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Cadastrar Indústria",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            try {
                clientController.registerClient(
                        txtCorporateName.getText(),
                        txtFantasyName.getText(),
                        txtCnpj.getText(),
                        txtPhone.getText(),
                        txtEmail.getText(),
                        cbCompanyLink.getSelectedItem().toString()
                );

                JOptionPane.showMessageDialog(this, "Indústria cadastrada com sucesso.");
                loadClients();

            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadClients() {
        try {
            fillTable(clientController.listAll());
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyFilters() {
        String search = txtSearch.getText().trim().toLowerCase();
        String status = cbStatusFilter.getSelectedItem().toString();
        String company = cbCompanyFilter.getSelectedItem().toString();

        List<Client> clients = clientController.listAll();

        List<Client> filtered = clients.stream()
                .filter(c -> search.isEmpty() || matchesClientSearch(c, search))
                .filter(c -> status.equals("Todos")
                        || (status.equals("Ativos") && c.isActive())
                        || (status.equals("Inativos") && !c.isActive()))
                .filter(c -> company.equals("Todos") || company.equals(c.getCompanyLink()))
                .toList();

        fillTable(filtered);
    }

    private boolean matchesClientSearch(Client client, String search) {
        return containsIgnoreCase(client.getName(), search)
                || containsIgnoreCase(client.getCorporateName(), search)
                || containsIgnoreCase(client.getCnpj(), search);
    }

    private boolean containsIgnoreCase(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    private void resetFilters() {
        txtSearch.setText("");
        cbStatusFilter.setSelectedIndex(0);
        cbCompanyFilter.setSelectedIndex(0);
        loadClients();
    }

    private void activateClient() {
        int id = getSelectedClientId();

        if (id == -1) {
            return;
        }

        clientController.activateClient(id);
        loadClients();
    }

    private void deactivateClient() {
        int id = getSelectedClientId();

        if (id == -1) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Deseja inativar esta indústria?",
                "Confirmação",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            clientController.deactivateClient(id);
            loadClients();
        }
    }

    private void openEditDialog() {
        int id = getSelectedClientId();

        if (id == -1) {
            return;
        }

        try {
            Client client = clientController.findById(id);

            JTextField txtCorporateName = new JTextField(formatField(client.getCorporateName()));
            JTextField txtFantasyName = new JTextField(formatField(client.getName()));
            JTextField txtCnpj = new JTextField(formatField(client.getCnpj()));
            JTextField txtPhone = new JTextField(formatField(client.getPhone()));
            JTextField txtEmail = new JTextField(formatField(client.getEmail()));

            JComboBox<String> cbCompanyLink = new JComboBox<>(new String[]{"AT", "TEJO"});
            cbCompanyLink.setSelectedItem(client.getCompanyLink());

            JComboBox<String> cbStatus = new JComboBox<>(new String[]{"Ativo", "Inativo"});
            cbStatus.setSelectedItem(client.isActive() ? "Ativo" : "Inativo");

            JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
            panel.add(new JLabel("Razão social:"));
            panel.add(txtCorporateName);
            panel.add(new JLabel("Nome fantasia:"));
            panel.add(txtFantasyName);
            panel.add(new JLabel("CNPJ:"));
            panel.add(txtCnpj);
            panel.add(new JLabel("Telefone:"));
            panel.add(txtPhone);
            panel.add(new JLabel("Email:"));
            panel.add(txtEmail);
            panel.add(new JLabel("Vínculo:"));
            panel.add(cbCompanyLink);
            panel.add(new JLabel("Status:"));
            panel.add(cbStatus);

            int result = JOptionPane.showConfirmDialog(
                    this,
                    panel,
                    "Editar Indústria",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result == JOptionPane.OK_OPTION) {
                clientController.updateClient(
                        id,
                        txtCorporateName.getText(),
                        txtFantasyName.getText(),
                        txtCnpj.getText(),
                        txtPhone.getText(),
                        txtEmail.getText(),
                        cbCompanyLink.getSelectedItem().toString(),
                        cbStatus.getSelectedItem().toString().equals("Ativo")
                );

                JOptionPane.showMessageDialog(this, "Indústria atualizada com sucesso.");
                loadClients();
            }

        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String formatField(String value) {
        if (value == null) {
            return "";
        }

        return value;
    }

    private void showClientDetails() {
        int id = getSelectedClientId();

        if (id == -1) {
            return;
        }

        try {
            Client client = clientController.findById(id);

            JDialog dialog = new JDialog(this, "Detalhes da Indústria", true);
            dialog.setSize(560, 550);
            dialog.setLocationRelativeTo(this);
            dialog.setResizable(false);
            dialog.setLayout(new BorderLayout());
            dialog.getContentPane().setBackground(WHITE);

            dialog.add(createClientDetailsHeader(client), BorderLayout.NORTH);
            dialog.add(createClientDetailsBody(client), BorderLayout.CENTER);
            dialog.add(createClientDetailsFooter(dialog), BorderLayout.SOUTH);

            dialog.setVisible(true);

        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createClientDetailsHeader(Client client) {
        JPanel header = new JPanel(null);
        header.setPreferredSize(new Dimension(560, 115));
        header.setBackground(BLACK);

        JLabel title = new JLabel(formatDetail(client.getName()));
        title.setForeground(WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setBounds(28, 20, 365, 32);
        header.add(title);

        JLabel subtitle = new JLabel(formatDetail(client.getCorporateName()));
        subtitle.setForeground(new Color(210, 210, 210));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setBounds(30, 55, 370, 24);
        header.add(subtitle);

        JLabel status = new JLabel(client.isActive() ? "ATIVO" : "INATIVO", SwingConstants.CENTER);
        status.setOpaque(true);
        status.setBackground(client.isActive() ? GREEN : RED);
        status.setForeground(WHITE);
        status.setFont(new Font("Segoe UI", Font.BOLD, 12));
        status.setBounds(420, 30, 95, 30);
        header.add(status);

        JPanel orangeLine = new JPanel();
        orangeLine.setBackground(ORANGE);
        orangeLine.setBounds(28, 92, 485, 4);
        header.add(orangeLine);

        return header;
    }

    private JPanel createClientDetailsBody(Client client) {
        JPanel body = new JPanel(null);
        body.setBackground(WHITE);

        JPanel card = new JPanel(new GridLayout(0, 1, 0, 10));
        card.setBackground(WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 225, 225)),
                BorderFactory.createEmptyBorder(18, 22, 18, 22)
        ));
        card.setBounds(28, 24, 485, 300);

        card.add(createDetailLine("Razão Social", client.getCorporateName()));
        card.add(createDetailLine("Nome Fantasia", client.getName()));
        card.add(createDetailLine("CNPJ", client.getCnpj()));
        card.add(createDetailLine("Telefone", client.getPhone()));
        card.add(createDetailLine("Email", client.getEmail()));
        card.add(createDetailLine("Vínculo", client.getCompanyLink()));
        card.add(createDetailLine("Status", client.isActive() ? "Ativo" : "Inativo"));

        body.add(card);

        return body;
    }

    private JPanel createDetailLine(String label, String value) {
        JPanel line = new JPanel(new BorderLayout());
        line.setBackground(WHITE);

        JLabel labelText = new JLabel(label);
        labelText.setForeground(new Color(110, 110, 110));
        labelText.setFont(new Font("Segoe UI", Font.BOLD, 12));
        labelText.setPreferredSize(new Dimension(120, 28));

        JLabel valueText = new JLabel(formatDetail(value));
        valueText.setForeground(BLACK);
        valueText.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        line.add(labelText, BorderLayout.WEST);
        line.add(valueText, BorderLayout.CENTER);

        return line;
    }

    private JPanel createClientDetailsFooter(JDialog dialog) {
        JPanel footer = new JPanel(null);
        footer.setPreferredSize(new Dimension(560, 70));
        footer.setBackground(LIGHT_GRAY);

        JButton btnClose = createDarkButton("Fechar");
        btnClose.setBounds(395, 18, 120, 34);
        btnClose.addActionListener(e -> dialog.dispose());
        footer.add(btnClose);

        return footer;
    }

    private String formatDetail(String value) {
        if (value == null || value.isBlank()) {
            return "Não informado";
        }

        return value;
    }

    private int getSelectedClientId() {
        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Selecione uma indústria na tabela.");
            return -1;
        }

        return (int) tableModel.getValueAt(row, 0);
    }

    private void fillTable(List<Client> clients) {
        tableModel.setRowCount(0);

        for (Client client : clients) {
            tableModel.addRow(new Object[]{
                    client.getId(),
                    client.getName(),
                    client.getCnpj(),
                    client.getPhone(),
                    client.getEmail(),
                    client.getCompanyLink(),
                    client.isActive() ? "Ativo" : "Inativo"
            });
        }
    }

    private void exportExcel() {
        try {
            List<Client> clients = getCurrentTableClients();

            if (clients.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nenhum cliente para exportar.");
                return;
            }

            String path = util.FileSaveDialog.chooseXlsxPath(this, "clientes_industrias.xlsx");

            if (path == null) {
                return;
            }

            util.ExcelGenerator.generateClients(clients, path);

            JOptionPane.showMessageDialog(this, "Excel de clientes gerado com sucesso.");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<Client> getCurrentTableClients() {
        String search = txtSearch.getText().trim().toLowerCase();
        String status = cbStatusFilter.getSelectedItem().toString();
        String company = cbCompanyFilter.getSelectedItem().toString();

        List<Client> clients = clientController.listAll();

        return clients.stream()
                .filter(c -> search.isEmpty() || matchesClientSearch(c, search))
                .filter(c -> status.equals("Todos")
                        || (status.equals("Ativos") && c.isActive())
                        || (status.equals("Inativos") && !c.isActive()))
                .filter(c -> company.equals("Todos") || company.equals(c.getCompanyLink()))
                .toList();
    }

    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(ORANGE);
        button.setForeground(WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton createDarkButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(BLACK);
        button.setForeground(WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(WHITE);
        button.setForeground(BLACK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton createDangerButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(RED);
        button.setForeground(WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }
}