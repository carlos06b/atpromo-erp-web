package view;

import controller.ClientController;
import controller.InvoiceController;
import model.Client;
import model.Invoice;
import model.InvoiceView;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class InvoiceFrame extends JFrame {

    private final InvoiceController invoiceController = new InvoiceController();
    private final ClientController clientController = new ClientController();

    private final DateTimeFormatter BR_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final Locale BR_LOCALE = new Locale("pt", "BR");
    private final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(BR_LOCALE);

    private final Color ORANGE = new Color(255, 102, 0);
    private final Color BLACK = new Color(18, 18, 18);
    private final Color WHITE = Color.WHITE;
    private final Color LIGHT_GRAY = new Color(245, 245, 245);

    private JTextField txtStartDate;
    private JTextField txtEndDate;

    private JComboBox<String> cbStatusFilter;
    private JComboBox<String> cbCompanyFilter;

    private JTable table;
    private DefaultTableModel tableModel;

    private JLabel lblPending;
    private JLabel lblIssued;
    private JLabel lblPaid;
    private JLabel lblCanceled;

    public InvoiceFrame() {
        setTitle("Sistema At Promo - Faturamento");
        setSize(1120, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel mainPanel = new JPanel(null);
        mainPanel.setBackground(LIGHT_GRAY);
        add(mainPanel);

        createHeader(mainPanel);
        createFilterPanel(mainPanel);
        createDashboard(mainPanel);
        createTable(mainPanel);
        createActionButtons(mainPanel);

        loadInvoices();

        setVisible(true);
    }

    private void createHeader(JPanel panel) {
        JPanel header = new JPanel(null);
        header.setBackground(BLACK);
        header.setBounds(0, 0, 1120, 95);
        panel.add(header);

        JLabel title = new JLabel("Faturamento");
        title.setForeground(WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setBounds(30, 18, 400, 35);
        header.add(title);

        JLabel subtitle = new JLabel("Controle de cobranças pendentes, faturadas, recebidas e canceladas");
        subtitle.setForeground(new Color(210, 210, 210));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        subtitle.setBounds(32, 55, 760, 25);
        header.add(subtitle);

        JPanel orangeLine = new JPanel();
        orangeLine.setBackground(ORANGE);
        orangeLine.setBounds(30, 85, 1040, 4);
        header.add(orangeLine);
    }

    private void createFilterPanel(JPanel panel) {
        JPanel filterPanel = new JPanel(null);
        filterPanel.setBackground(WHITE);
        filterPanel.setBounds(30, 120, 1040, 95);
        filterPanel.setBorder(BorderFactory.createLineBorder(new Color(225, 225, 225)));
        panel.add(filterPanel);

        JLabel lblStart = new JLabel("Data inicial");
        lblStart.setBounds(20, 12, 120, 20);
        filterPanel.add(lblStart);

        txtStartDate = new JTextField(LocalDate.now().withDayOfMonth(1).format(BR_FORMAT));
        txtStartDate.setBounds(20, 38, 120, 32);
        filterPanel.add(txtStartDate);

        JLabel lblEnd = new JLabel("Data final");
        lblEnd.setBounds(155, 12, 120, 20);
        filterPanel.add(lblEnd);

        txtEndDate = new JTextField(LocalDate.now().format(BR_FORMAT));
        txtEndDate.setBounds(155, 38, 120, 32);
        filterPanel.add(txtEndDate);

        JLabel lblStatus = new JLabel("Status");
        lblStatus.setBounds(295, 12, 120, 20);
        filterPanel.add(lblStatus);

        cbStatusFilter = new JComboBox<>(new String[]{"Todos", "PENDENTE", "FATURADO", "PAGO", "CANCELADO"});
        cbStatusFilter.setBounds(295, 38, 140, 32);
        filterPanel.add(cbStatusFilter);

        JLabel lblCompany = new JLabel("Vínculo");
        lblCompany.setBounds(455, 12, 120, 20);
        filterPanel.add(lblCompany);

        cbCompanyFilter = new JComboBox<>(new String[]{"Todos", "AT", "TEJO"});
        cbCompanyFilter.setBounds(455, 38, 120, 32);
        filterPanel.add(cbCompanyFilter);

        JButton btnFilter = createDarkButton("Filtrar");
        btnFilter.setBounds(605, 38, 100, 32);
        btnFilter.addActionListener(e -> applyFilters());
        filterPanel.add(btnFilter);

        JButton btnRefresh = createSecondaryButton("Atualizar");
        btnRefresh.setBounds(715, 38, 110, 32);
        btnRefresh.addActionListener(e -> resetFilters());
        filterPanel.add(btnRefresh);

        JButton btnNew = createPrimaryButton("Novo faturamento");
        btnNew.setBounds(840, 38, 170, 32);
        btnNew.addActionListener(e -> openRegisterDialog());
        filterPanel.add(btnNew);
    }

    private void createDashboard(JPanel panel) {
        JPanel dashboard = new JPanel(null);
        dashboard.setBackground(WHITE);
        dashboard.setBounds(30, 230, 1040, 78);
        dashboard.setBorder(BorderFactory.createLineBorder(new Color(225, 225, 225)));
        panel.add(dashboard);

        lblPending = createDashboardLabel("Pendente: R$ 0,00", 25);
        lblIssued = createDashboardLabel("Faturado: R$ 0,00", 285);
        lblPaid = createDashboardLabel("Recebido: R$ 0,00", 545);
        lblCanceled = createDashboardLabel("Cancelado: 0 registros", 805);

        dashboard.add(lblPending);
        dashboard.add(lblIssued);
        dashboard.add(lblPaid);
        dashboard.add(lblCanceled);
    }

    private JLabel createDashboardLabel(String text, int x) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 15));
        label.setForeground(BLACK);
        label.setBounds(x, 25, 240, 30);
        return label;
    }

    private void createTable(JPanel panel) {
        tableModel = new DefaultTableModel(
                new Object[]{
                        "ID", "Indústria", "Vínculo", "Faturado", "Recebido",
                        "Descrição", "Previsto", "Faturado em", "Recebido em", "Status"
                }, 0
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

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                Component component = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column
                );

                if (component instanceof JLabel label) {
                    label.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                    label.setHorizontalAlignment(
                            column == 3 || column == 4 ? SwingConstants.RIGHT : SwingConstants.LEFT
                    );
                }

                String status = table.getValueAt(row, 9).toString();

                if (isSelected) {
                    component.setBackground(new Color(210, 230, 255));
                    component.setForeground(Color.BLACK);
                    return component;
                }

                switch (status) {
                    case "PENDENTE" -> component.setBackground(new Color(255, 248, 220));
                    case "FATURADO" -> component.setBackground(new Color(255, 235, 205));
                    case "PAGO" -> component.setBackground(new Color(220, 245, 220));
                    case "CANCELADO" -> component.setBackground(new Color(245, 220, 220));
                    default -> component.setBackground(Color.WHITE);
                }

                component.setForeground(Color.BLACK);
                return component;
            }
        });

        JTableHeader header = table.getTableHeader();
        header.setBackground(BLACK);
        header.setForeground(WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBounds(30, 325, 1040, 310);
        panel.add(scrollPane);

        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);

        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(65);
        table.getColumnModel().getColumn(3).setPreferredWidth(105);
        table.getColumnModel().getColumn(4).setPreferredWidth(105);
        table.getColumnModel().getColumn(5).setPreferredWidth(190);
        table.getColumnModel().getColumn(6).setPreferredWidth(90);
        table.getColumnModel().getColumn(7).setPreferredWidth(105);
        table.getColumnModel().getColumn(8).setPreferredWidth(105);
        table.getColumnModel().getColumn(9).setPreferredWidth(90);
    }

    private void createActionButtons(JPanel panel) {
        JButton btnEdit = createSecondaryButton("Editar");
        btnEdit.setBounds(70, 655, 120, 38);
        btnEdit.addActionListener(e -> openEditDialog());
        panel.add(btnEdit);

        JButton btnCancel = createDangerButton("Cancelar");
        btnCancel.setBounds(200, 655, 120, 38);
        btnCancel.addActionListener(e -> cancelInvoice());
        panel.add(btnCancel);

        JButton btnIssue = createPrimaryButton("Faturar");
        btnIssue.setBounds(330, 655, 120, 38);
        btnIssue.addActionListener(e -> markAsIssued());
        panel.add(btnIssue);

        JButton btnPaid = createPrimaryButton("Receber");
        btnPaid.setBounds(460, 655, 120, 38);
        btnPaid.addActionListener(e -> markAsPaid());
        panel.add(btnPaid);

        JButton btnExcel = createPrimaryButton("Exportar Excel");
        btnExcel.setBounds(590, 655, 170, 38);
        btnExcel.addActionListener(e -> exportExcel());
        panel.add(btnExcel);

        JButton btnClose = createDarkButton("Fechar");
        btnClose.setBounds(775, 655, 120, 38);
        btnClose.addActionListener(e -> dispose());
        panel.add(btnClose);
    }

    private void openRegisterDialog() {
        List<Client> clients = clientController.listActiveClients();

        if (clients.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhuma indústria ativa cadastrada.");
            return;
        }

        JComboBox<ClientComboItem> cbClient = new JComboBox<>();

        for (Client client : clients) {
            cbClient.addItem(new ClientComboItem(client.getId(), client.getName(), client.getCompanyLink()));
        }

        JTextField txtAmount = new JTextField();
        JTextField txtDescription = new JTextField();
        JTextField txtDueDate = new JTextField(LocalDate.now().format(BR_FORMAT));

        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.add(new JLabel("Indústria:"));
        panel.add(cbClient);
        panel.add(new JLabel("Valor faturado:"));
        panel.add(txtAmount);
        panel.add(new JLabel("Descrição:"));
        panel.add(txtDescription);
        panel.add(new JLabel("Data prevista (dd/MM/aaaa):"));
        panel.add(txtDueDate);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Novo Faturamento",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            try {
                ClientComboItem selectedClient = (ClientComboItem) cbClient.getSelectedItem();

                if (selectedClient == null) {
                    throw new RuntimeException("Selecione uma indústria.");
                }

                BigDecimal amount = parseMoney(txtAmount.getText());
                LocalDate dueDate = parseBrazilianDate(txtDueDate.getText());

                invoiceController.createPendingInvoice(
                        selectedClient.getId(),
                        amount,
                        txtDescription.getText(),
                        dueDate
                );

                JOptionPane.showMessageDialog(this, "Faturamento pendente criado com sucesso.");
                loadInvoices();

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openEditDialog() {
        int id = getSelectedInvoiceId();

        if (id == -1) {
            return;
        }

        try {
            Invoice invoice = invoiceController.findById(id);

            if ("PAGO".equals(invoice.getStatus()) || "CANCELADO".equals(invoice.getStatus())) {
                JOptionPane.showMessageDialog(
                        this,
                        "Faturamentos pagos ou cancelados não podem ser editados.",
                        "Atenção",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            List<Client> clients = clientController.listActiveClients();

            if (clients.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nenhuma indústria ativa cadastrada.");
                return;
            }

            JComboBox<ClientComboItem> cbClient = new JComboBox<>();
            ClientComboItem selectedItem = null;

            for (Client client : clients) {
                ClientComboItem item = new ClientComboItem(
                        client.getId(),
                        client.getName(),
                        client.getCompanyLink()
                );

                cbClient.addItem(item);

                if (client.getId() == invoice.getClientId()) {
                    selectedItem = item;
                }
            }

            if (selectedItem != null) {
                cbClient.setSelectedItem(selectedItem);
            }

            JTextField txtAmount = new JTextField(moneyForInput(invoice.getAmount()));
            JTextField txtDescription = new JTextField(formatField(invoice.getDescription()));
            JTextField txtDueDate = new JTextField(formatDate(invoice.getDueDate()));

            JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
            panel.add(new JLabel("Indústria:"));
            panel.add(cbClient);
            panel.add(new JLabel("Valor faturado:"));
            panel.add(txtAmount);
            panel.add(new JLabel("Descrição:"));
            panel.add(txtDescription);
            panel.add(new JLabel("Data prevista (dd/MM/aaaa):"));
            panel.add(txtDueDate);

            int result = JOptionPane.showConfirmDialog(
                    this,
                    panel,
                    "Editar Faturamento",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result == JOptionPane.OK_OPTION) {
                ClientComboItem selectedClient = (ClientComboItem) cbClient.getSelectedItem();

                if (selectedClient == null) {
                    throw new RuntimeException("Selecione uma indústria.");
                }

                BigDecimal amount = parseMoney(txtAmount.getText());
                LocalDate dueDate = parseBrazilianDate(txtDueDate.getText());

                invoiceController.updateInvoice(
                        id,
                        selectedClient.getId(),
                        amount,
                        txtDescription.getText(),
                        dueDate
                );

                JOptionPane.showMessageDialog(this, "Faturamento atualizado com sucesso.");
                loadInvoices();
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadInvoices() {
        try {
            LocalDate start = parseBrazilianDate(txtStartDate.getText());
            LocalDate end = parseBrazilianDate(txtEndDate.getText());

            fillTable(invoiceController.listByPeriod(start, end));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar faturamentos: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyFilters() {
        try {
            LocalDate start = parseBrazilianDate(txtStartDate.getText());
            LocalDate end = parseBrazilianDate(txtEndDate.getText());

            String status = cbStatusFilter.getSelectedItem().toString();
            String company = cbCompanyFilter.getSelectedItem().toString();

            fillTable(invoiceController.listByFilters(start, end, status, company));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao filtrar faturamentos: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetFilters() {
        txtStartDate.setText(LocalDate.now().withDayOfMonth(1).format(BR_FORMAT));
        txtEndDate.setText(LocalDate.now().format(BR_FORMAT));
        cbStatusFilter.setSelectedIndex(0);
        cbCompanyFilter.setSelectedIndex(0);
        loadInvoices();
    }

    private void markAsIssued() {
        int id = getSelectedInvoiceId();

        if (id == -1) {
            return;
        }

        String issueDateText = JOptionPane.showInputDialog(
                this,
                "Informe a data de faturamento (dd/MM/aaaa):",
                LocalDate.now().format(BR_FORMAT)
        );

        if (issueDateText == null || issueDateText.trim().isEmpty()) {
            return;
        }

        try {
            LocalDate issueDate = parseBrazilianDate(issueDateText.trim());

            invoiceController.markAsIssued(id, issueDate);

            JOptionPane.showMessageDialog(this, "Faturamento marcado como faturado.");
            loadInvoices();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void markAsPaid() {
        int id = getSelectedInvoiceId();

        if (id == -1) {
            return;
        }

        try {
            Invoice invoice = invoiceController.findById(id);

            JTextField txtPaymentDate = new JTextField(LocalDate.now().format(BR_FORMAT));
            JTextField txtReceivedAmount = new JTextField(
                    moneyForInput(invoice.getReceivedAmount() != null ? invoice.getReceivedAmount() : invoice.getAmount())
            );

            JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
            panel.add(new JLabel("Data de recebimento (dd/MM/aaaa):"));
            panel.add(txtPaymentDate);
            panel.add(new JLabel("Valor recebido:"));
            panel.add(txtReceivedAmount);

            int result = JOptionPane.showConfirmDialog(
                    this,
                    panel,
                    "Marcar como recebido",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result == JOptionPane.OK_OPTION) {
                LocalDate paymentDate = parseBrazilianDate(txtPaymentDate.getText());
                BigDecimal receivedAmount = parseMoney(txtReceivedAmount.getText());

                invoiceController.markAsPaid(id, paymentDate, receivedAmount);

                JOptionPane.showMessageDialog(this, "Faturamento marcado como recebido.");
                loadInvoices();
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelInvoice() {
        int id = getSelectedInvoiceId();

        if (id == -1) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Deseja cancelar este faturamento?",
                "Confirmação",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                invoiceController.cancelInvoice(id);
                JOptionPane.showMessageDialog(this, "Faturamento cancelado.");
                loadInvoices();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private int getSelectedInvoiceId() {
        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um faturamento na tabela.");
            return -1;
        }

        return (int) tableModel.getValueAt(row, 0);
    }

    private void fillTable(List<InvoiceView> invoices) {
        tableModel.setRowCount(0);

        for (InvoiceView invoice : invoices) {
            tableModel.addRow(new Object[]{
                    invoice.getId(),
                    invoice.getClientName(),
                    invoice.getCompanyLink(),
                    formatMoney(invoice.getAmount()),
                    formatReceivedAmount(invoice),
                    invoice.getDescription(),
                    formatDate(invoice.getDueDate()),
                    formatDate(invoice.getIssueDate()),
                    formatDate(invoice.getPaymentDate()),
                    invoice.getStatus()
            });
        }

        updateDashboard(invoices);
    }

    private void updateDashboard(List<InvoiceView> invoices) {
        BigDecimal pending = BigDecimal.ZERO;
        BigDecimal issued = BigDecimal.ZERO;
        BigDecimal received = BigDecimal.ZERO;
        int canceledCount = 0;

        for (InvoiceView invoice : invoices) {
            if (invoice.getAmount() == null || invoice.getStatus() == null) {
                continue;
            }

            switch (invoice.getStatus()) {
                case "PENDENTE" -> pending = pending.add(invoice.getAmount());

                case "FATURADO" -> issued = issued.add(invoice.getAmount());

                case "PAGO" -> {
                    issued = issued.add(invoice.getAmount());
                    received = received.add(getReceivedAmount(invoice));
                }

                case "CANCELADO" -> canceledCount++;
            }
        }

        lblPending.setText("Pendente: " + formatMoney(pending));
        lblIssued.setText("Faturado: " + formatMoney(issued));
        lblPaid.setText("Recebido: " + formatMoney(received));
        lblCanceled.setText("Cancelado: " + canceledCount + " registros");
    }

    private BigDecimal getReceivedAmount(InvoiceView invoice) {
        if (invoice.getReceivedAmount() != null) {
            return invoice.getReceivedAmount();
        }

        return invoice.getAmount() != null ? invoice.getAmount() : BigDecimal.ZERO;
    }

    private String formatReceivedAmount(InvoiceView invoice) {
        if (!"PAGO".equals(invoice.getStatus())) {
            return "-";
        }

        return formatMoney(getReceivedAmount(invoice));
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "R$ 0,00";
        }

        return MONEY_FORMAT.format(value.setScale(2, RoundingMode.HALF_UP))
                .replace('\u00A0', ' ');
    }

    private String moneyForInput(BigDecimal value) {
        if (value == null) {
            return "";
        }

        return formatMoney(value)
                .replace("R$", "")
                .trim();
    }

    private BigDecimal parseMoney(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Informe um valor válido.");
        }

        String cleanValue = value
                .replace("R$", "")
                .replace(" ", "")
                .replace("\u00A0", "")
                .trim();

        int lastComma = cleanValue.lastIndexOf(',');
        int lastDot = cleanValue.lastIndexOf('.');

        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                cleanValue = cleanValue.replace(".", "").replace(",", ".");
            } else {
                cleanValue = cleanValue.replace(",", "");
            }
        } else if (lastComma >= 0) {
            cleanValue = cleanValue.replace(".", "").replace(",", ".");
        } else if (lastDot >= 0) {
            int firstDot = cleanValue.indexOf('.');
            int digitsAfterDot = cleanValue.length() - lastDot - 1;

            if (firstDot != lastDot || digitsAfterDot > 2) {
                cleanValue = cleanValue.replace(".", "");
            }
        }

        try {
            return new BigDecimal(cleanValue).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Valor inválido. Use o formato 130000,00 ou 130.000,00.");
        }
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "-";
        }

        return date.format(BR_FORMAT);
    }

    private String formatField(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value;
    }

    private LocalDate parseBrazilianDate(String date) {
        try {
            return LocalDate.parse(date.trim(), BR_FORMAT);
        } catch (Exception e) {
            throw new RuntimeException("Data inválida. Use o formato dd/MM/aaaa.");
        }
    }

    private void exportExcel() {
        try {
            LocalDate start = parseBrazilianDate(txtStartDate.getText());
            LocalDate end = parseBrazilianDate(txtEndDate.getText());

            String status = cbStatusFilter.getSelectedItem().toString();
            String company = cbCompanyFilter.getSelectedItem().toString();

            List<InvoiceView> invoices = invoiceController.listByFilters(start, end, status, company);

            if (invoices.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nenhum dado para exportar.");
                return;
            }

            String path = util.FileSaveDialog.chooseXlsxPath(this, "faturamento.xlsx");

            if (path == null) {
                return;
            }

            util.ExcelGenerator.generateInvoices(invoices, path);

            JOptionPane.showMessageDialog(this, "Excel gerado com sucesso!");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
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

    private JButton createDangerButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(180, 40, 40));
        button.setForeground(Color.WHITE);
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

    private static class ClientComboItem {
        private final int id;
        private final String name;
        private final String companyLink;

        public ClientComboItem(int id, String name, String companyLink) {
            this.id = id;
            this.name = name;
            this.companyLink = companyLink;
        }

        public int getId() {
            return id;
        }

        @Override
        public String toString() {
            return name + " - " + companyLink;
        }
    }
}