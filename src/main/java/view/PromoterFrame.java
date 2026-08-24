package view;

import controller.PromoterController;
import model.Promoter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PromoterFrame extends JFrame {

    private final PromoterController promoterController;

    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;

    private final Color ORANGE = new Color(255, 102, 0);
    private final Color BLACK = new Color(18, 18, 18);
    private final Color WHITE = Color.WHITE;
    private final Color LIGHT_GRAY = new Color(245, 245, 245);
    private final Color BORDER_GRAY = new Color(220, 220, 220);
    private final Color TEXT_GRAY = new Color(90, 90, 90);
    private final Color RED = new Color(190, 40, 40);

    private final Locale brLocale = new Locale("pt", "BR");
    private final NumberFormat moneyFormatter = NumberFormat.getCurrencyInstance(brLocale);

    public PromoterFrame() {
        promoterController = new PromoterController();

        setTitle("Sistema At Promo - Promotores");
        setSize(1220, 720);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createMainPanel(), BorderLayout.CENTER);

        loadPromoters();

        setVisible(true);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(null);
        header.setPreferredSize(new Dimension(1220, 95));
        header.setBackground(BLACK);

        JLabel title = new JLabel("Gerenciamento de Promotores");
        title.setForeground(WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setBounds(30, 20, 450, 34);
        header.add(title);

        JLabel subtitle = new JLabel("Cadastre, filtre, busque, edite e controle o status dos promotores da empresa.");
        subtitle.setForeground(new Color(210, 210, 210));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setBounds(32, 56, 760, 22);
        header.add(subtitle);

        JPanel line = new JPanel();
        line.setBackground(ORANGE);
        line.setBounds(30, 82, 210, 4);
        header.add(line);

        return header;
    }

    private JPanel createMainPanel() {
        JPanel main = new JPanel(null);
        main.setBackground(LIGHT_GRAY);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
        actionPanel.setBackground(WHITE);
        actionPanel.setBorder(BorderFactory.createLineBorder(BORDER_GRAY));
        actionPanel.setBounds(30, 20, 1145, 65);
        main.add(actionPanel);

        JButton btnList = createDarkButton("Listar");
        JButton btnFilter = createSecondaryButton("Filtrar");
        JButton btnSearch = createSecondaryButton("Buscar");
        JButton btnInactive = createDangerButton("Inativar");
        JButton btnActivate = createPrimaryButton("Ativar");
        JButton btnRegister = createPrimaryButton("Cadastrar");
        JButton btnDetails = createDarkButton("Detalhes");
        JButton btnExportExcel = createPrimaryButton("Exportar Excel");

        actionPanel.add(btnList);
        actionPanel.add(btnFilter);
        actionPanel.add(btnSearch);
        actionPanel.add(btnInactive);
        actionPanel.add(btnActivate);
        actionPanel.add(btnRegister);
        actionPanel.add(btnDetails);
        actionPanel.add(btnExportExcel);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(WHITE);
        tablePanel.setBorder(BorderFactory.createLineBorder(BORDER_GRAY));
        tablePanel.setBounds(30, 115, 1145, 430);
        main.add(tablePanel);

        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setBackground(WHITE);
        tableHeader.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JLabel tableTitle = new JLabel("Lista de promotores");
        tableTitle.setForeground(BLACK);
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        tableHeader.add(tableTitle, BorderLayout.WEST);

        statusLabel = new JLabel("Carregando...");
        statusLabel.setForeground(TEXT_GRAY);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableHeader.add(statusLabel, BorderLayout.EAST);

        tablePanel.add(tableHeader, BorderLayout.NORTH);

        String[] columns = {
                "ID", "Nome", "CPF", "Telefone", "UF", "Cidade",
                "Vínculo", "Tipo", "Salário", "Status", "Editar"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) {
                return c == 10;
            }
        };

        table = createStyledTable();
        table.setModel(tableModel);

        table.getColumn("Editar").setCellRenderer(new ButtonRenderer());
        table.getColumn("Editar").setCellEditor(new ButtonEditor(new JCheckBox(), this));

        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);

        table.getColumnModel().getColumn(1).setPreferredWidth(190);
        table.getColumnModel().getColumn(2).setPreferredWidth(115);
        table.getColumnModel().getColumn(3).setPreferredWidth(135);
        table.getColumnModel().getColumn(4).setPreferredWidth(45);
        table.getColumnModel().getColumn(5).setPreferredWidth(130);
        table.getColumnModel().getColumn(6).setPreferredWidth(80);
        table.getColumnModel().getColumn(7).setPreferredWidth(75);
        table.getColumnModel().getColumn(8).setPreferredWidth(105);
        table.getColumnModel().getColumn(9).setPreferredWidth(85);
        table.getColumnModel().getColumn(10).setPreferredWidth(85);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(12);

        tablePanel.add(scroll, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel(null);
        infoPanel.setBackground(WHITE);
        infoPanel.setBorder(BorderFactory.createLineBorder(BORDER_GRAY));
        infoPanel.setBounds(30, 565, 1145, 55);
        main.add(infoPanel);

        JLabel infoText = new JLabel("Dica: use Buscar para localizar rapidamente pelo nome. Use o botão Editar para alterar os dados do promotor.");
        infoText.setForeground(TEXT_GRAY);
        infoText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        infoText.setBounds(18, 15, 950, 25);
        infoPanel.add(infoText);

        btnList.addActionListener(e -> loadPromoters());
        btnFilter.addActionListener(e -> openFilterDialog());
        btnSearch.addActionListener(e -> searchByName());
        btnInactive.addActionListener(e -> actionInactivate());
        btnActivate.addActionListener(e -> actionActivate());
        btnRegister.addActionListener(e -> openRegisterDialog());
        btnDetails.addActionListener(e -> showPromoterDetails());
        btnExportExcel.addActionListener(e -> exportExcel());

        return main;
    }

    private JTable createStyledTable() {
        JTable styledTable = new JTable();
        styledTable.setRowHeight(30);
        styledTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        styledTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        styledTable.getTableHeader().setBackground(BLACK);
        styledTable.getTableHeader().setForeground(WHITE);
        styledTable.setSelectionBackground(new Color(255, 225, 205));
        styledTable.setSelectionForeground(BLACK);
        styledTable.setGridColor(new Color(235, 235, 235));
        styledTable.setShowVerticalLines(false);
        return styledTable;
    }

    private void loadPromoters() {
        fillTable(promoterController.getAll());
        statusLabel.setText("Exibindo todos os promotores");
    }

    private void openFilterDialog() {
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"TODOS", "CLT", "MEI", "FERISTA"});
        JComboBox<String> statusBox = new JComboBox<>(new String[]{"TODOS", "ATIVO", "INATIVO"});
        JComboBox<String> ufBox = new JComboBox<>(new String[]{"TODOS", "MA", "PI", "CE", "RN", "PB", "PE", "AL", "SE", "BA"});
        JComboBox<String> companyBox = new JComboBox<>(new String[]{"TODOS", "AT", "TEJO"});

        styleComboBox(typeBox);
        styleComboBox(statusBox);
        styleComboBox(ufBox);
        styleComboBox(companyBox);

        Object[] fields = {
                "Tipo:", typeBox,
                "Status:", statusBox,
                "UF:", ufBox,
                "Vínculo:", companyBox
        };

        int option = JOptionPane.showConfirmDialog(
                this,
                fields,
                "Filtrar Promotores",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (option != JOptionPane.OK_OPTION) return;

        String type = typeBox.getSelectedItem().toString();
        String status = statusBox.getSelectedItem().toString();
        String uf = ufBox.getSelectedItem().toString();
        String company = companyBox.getSelectedItem().toString();

        List<Promoter> list = promoterController.getAll();

        List<Promoter> filtered = list.stream()
                .filter(p -> type.equals("TODOS") || (p.getType() != null && p.getType().equalsIgnoreCase(type)))
                .filter(p -> status.equals("TODOS")
                        || (status.equals("ATIVO") && p.isActive())
                        || (status.equals("INATIVO") && !p.isActive()))
                .filter(p -> uf.equals("TODOS") || (p.getUf() != null && p.getUf().equalsIgnoreCase(uf)))
                .filter(p -> company.equals("TODOS") || (p.getCompanyLink() != null && p.getCompanyLink().equalsIgnoreCase(company)))
                .toList();

        fillTable(filtered);
        statusLabel.setText("Filtro aplicado: " + type + " / " + status + " / " + uf + " / " + company);
    }

    private void searchByName() {
        JTextField searchField = createTextField();

        DefaultListModel<PromoterItem> listModel = new DefaultListModel<>();
        JList<PromoterItem> promoterList = new JList<>(listModel);
        promoterList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        promoterList.setSelectionBackground(new Color(255, 225, 205));
        promoterList.setSelectionForeground(BLACK);

        JScrollPane listScroll = new JScrollPane(promoterList);
        listScroll.setPreferredSize(new Dimension(340, 130));

        searchField.addCaretListener(e -> {
            String text = searchField.getText().trim();
            listModel.clear();

            if (text.length() < 2) return;

            List<Promoter> promoters = promoterController.searchByName(text);

            for (Promoter p : promoters) {
                listModel.addElement(new PromoterItem(p.getId(), p.getName()));
            }
        });

        Object[] fields = {
                "Buscar promotor pelo nome:", searchField,
                "Resultados:", listScroll
        };

        int option = JOptionPane.showConfirmDialog(
                this,
                fields,
                "Buscar Promotor",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (option == JOptionPane.OK_OPTION) {
            PromoterItem selected = promoterList.getSelectedValue();

            if (selected == null) {
                showWarning("Selecione um promotor.");
                return;
            }

            Promoter promoter = promoterController.findById(selected.getId());

            tableModel.setRowCount(0);

            if (promoter != null) {
                addRow(promoter);
                statusLabel.setText("Resultado da busca: " + promoter.getName());
            }
        }
    }

    private void actionInactivate() {
        int id = getSelectedId();

        if (id == -1) {
            showWarning("Selecione um promotor.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Deseja inativar este promotor?",
                "Confirmar inativação",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                promoterController.inactivate(id);
                loadPromoters();
                showSuccess("Promotor inativado com sucesso!");
            } catch (Exception e) {
                showError(e.getMessage());
            }
        }
    }

    private void actionActivate() {
        int id = getSelectedId();

        if (id == -1) {
            showWarning("Selecione um promotor.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Deseja ativar este promotor?",
                "Confirmar ativação",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                promoterController.activate(id);
                loadPromoters();
                showSuccess("Promotor ativado com sucesso!");
            } catch (Exception e) {
                showError(e.getMessage());
            }
        }
    }

    public void openEditDialog(int id) {
        Promoter p = promoterController.findById(id);

        if (p == null) {
            showWarning("Promotor não encontrado.");
            return;
        }

        JTextField name = createTextField(p.getName());

        JFormattedTextField phone = createPhoneField();
        phone.setText(formatPhone(p.getPhone()));

        JComboBox<String> ufBox = createUfComboBox();

        if (p.getUf() != null && !p.getUf().isBlank()) {
            ufBox.setSelectedItem(p.getUf());
        }

        JTextField city = createTextField(formatField(p.getCity()));

        JComboBox<String> companyLink = createCompanyLinkComboBox();

        if (p.getCompanyLink() != null && !p.getCompanyLink().isBlank()) {
            companyLink.setSelectedItem(p.getCompanyLink());
        }

        JTextField pix = createTextField(formatField(p.getPix()));
        JTextField salary = createTextField(moneyForInput(p.getSalary()));

        JComboBox<String> pixType = new JComboBox<>(new String[]{
                "TELEFONE", "EMAIL", "CPF", "CNPJ", "ALEATORIA"
        });
        pixType.setSelectedItem(p.getPixType());
        styleComboBox(pixType);

        JComboBox<String> type = new JComboBox<>(new String[]{"CLT", "MEI", "FERISTA"});
        type.setSelectedItem(p.getType());
        styleComboBox(type);

        Object[] fields = {
                "Nome:", name,
                "Telefone:", phone,
                "UF:", ufBox,
                "Cidade:", city,
                "Vínculo:", companyLink,
                "PIX:", pix,
                "Tipo do PIX:", pixType,
                "Salário:", salary,
                "Tipo:", type
        };

        int option = JOptionPane.showConfirmDialog(
                this,
                fields,
                "Editar Promotor",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (option == JOptionPane.OK_OPTION) {
            try {
                if (name.getText().trim().isBlank()
                        || cleanPhone(phone.getText()).isBlank()
                        || city.getText().trim().isBlank()) {
                    showWarning("Nome, telefone, UF e cidade são obrigatórios.");
                    return;
                }

                promoterController.update(
                        id,
                        name.getText().trim(),
                        cleanPhone(phone.getText()),
                        ufBox.getSelectedItem().toString(),
                        city.getText().trim(),
                        companyLink.getSelectedItem().toString(),
                        pix.getText().trim(),
                        pixType.getSelectedItem().toString(),
                        parseMoney(salary.getText()),
                        type.getSelectedItem().toString()
                );

                showSuccess("Promotor atualizado!");
                loadPromoters();

            } catch (Exception e) {
                showError(e.getMessage());
            }
        }
    }

    private void openRegisterDialog() {
        JTextField name = createTextField();
        JTextField cpf = createTextField();
        JFormattedTextField phone = createPhoneField();

        JComboBox<String> ufBox = createUfComboBox();
        JTextField city = createTextField();
        JComboBox<String> companyLink = createCompanyLinkComboBox();

        JTextField pix = createTextField();
        JTextField salary = createTextField();

        JComboBox<String> pixType = new JComboBox<>(new String[]{
                "TELEFONE", "EMAIL", "CPF", "CNPJ", "ALEATORIA"
        });
        styleComboBox(pixType);

        JSpinner birthSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor birthEditor = new JSpinner.DateEditor(birthSpinner, "dd/MM/yyyy");
        birthSpinner.setEditor(birthEditor);
        birthSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JComboBox<String> type = new JComboBox<>(new String[]{"CLT", "MEI", "FERISTA"});
        styleComboBox(type);

        Object[] fields = {
                "Nome:", name,
                "CPF:", cpf,
                "Telefone:", phone,
                "UF:", ufBox,
                "Cidade:", city,
                "Vínculo:", companyLink,
                "PIX:", pix,
                "Tipo do PIX:", pixType,
                "Nascimento:", birthSpinner,
                "Salário:", salary,
                "Tipo:", type
        };

        int option = JOptionPane.showConfirmDialog(
                this,
                fields,
                "Cadastrar Promotor",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (option == JOptionPane.OK_OPTION) {
            try {
                if (name.getText().trim().isBlank()
                        || cpf.getText().trim().isBlank()
                        || cleanPhone(phone.getText()).isBlank()
                        || city.getText().trim().isBlank()) {
                    showWarning("Nome, CPF, telefone, UF e cidade são obrigatórios.");
                    return;
                }

                Date birthDate = (Date) birthSpinner.getValue();

                LocalDate dateBirth = birthDate.toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();

                promoterController.register(
                        name.getText().trim(),
                        cpf.getText().trim(),
                        cleanPhone(phone.getText()),
                        ufBox.getSelectedItem().toString(),
                        city.getText().trim(),
                        companyLink.getSelectedItem().toString(),
                        pix.getText().trim(),
                        pixType.getSelectedItem().toString(),
                        dateBirth,
                        parseMoney(salary.getText()),
                        type.getSelectedItem().toString()
                );

                showSuccess("Promotor cadastrado!");
                loadPromoters();

            } catch (Exception e) {
                showError(e.getMessage());
            }
        }
    }

    private void fillTable(List<Promoter> list) {
        tableModel.setRowCount(0);

        for (Promoter p : list) {
            addRow(p);
        }

        statusLabel.setText(list.size() + " promotor(es) exibido(s)");
    }

    private void addRow(Promoter p) {
        tableModel.addRow(new Object[]{
                p.getId(),
                p.getName(),
                formatCpf(p.getCpf()),
                formatPhone(p.getPhone()),
                formatField(p.getUf()),
                formatField(p.getCity()),
                formatField(p.getCompanyLink()),
                p.getType(),
                formatMoney(p.getSalary()),
                p.isActive() ? "ATIVO" : "INATIVO",
                "Editar"
        });
    }

    private void exportExcel() {
        try {
            List<Promoter> promoters = getCurrentTablePromoters();

            if (promoters.isEmpty()) {
                showWarning("Nenhum promotor para exportar.");
                return;
            }

            String path = util.FileSaveDialog.chooseXlsxPath(this, "promotores.xlsx");

            if (path == null) {
                return;
            }

            util.ExcelGenerator.generatePromoters(promoters, path);

            showSuccess("Excel de promotores gerado com sucesso!");

        } catch (Exception e) {
            showError("Erro ao exportar promotores: " + e.getMessage());
        }
    }

    private List<Promoter> getCurrentTablePromoters() {
        List<Promoter> promoters = new ArrayList<>();

        for (int row = 0; row < tableModel.getRowCount(); row++) {
            int id = (int) tableModel.getValueAt(row, 0);
            Promoter promoter = promoterController.findById(id);

            if (promoter != null) {
                promoters.add(promoter);
            }
        }

        return promoters;
    }

    private void showPromoterDetails() {
        int id = getSelectedId();

        if (id == -1) {
            showWarning("Selecione um promotor.");
            return;
        }

        Promoter p = promoterController.findById(id);

        if (p == null) {
            showWarning("Promotor não encontrado.");
            return;
        }

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 18, 15, 18));

        JLabel title = new JLabel(p.getName());
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(BLACK);

        JLabel subtitle = new JLabel(formatField(p.getType()) + " - " + (p.isActive() ? "ATIVO" : "INATIVO"));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_GRAY);

        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setBackground(Color.WHITE);
        header.add(title);
        header.add(subtitle);

        JPanel info = new JPanel(new GridLayout(0, 2, 12, 10));
        info.setBackground(Color.WHITE);

        addDetail(info, "CPF", formatCpf(p.getCpf()));
        addDetail(info, "Telefone", formatPhone(p.getPhone()));
        addDetail(info, "UF", formatField(p.getUf()));
        addDetail(info, "Cidade", formatField(p.getCity()));
        addDetail(info, "Vínculo", formatField(p.getCompanyLink()));
        addDetail(info, "Nascimento", formatDate(p.getDateBirth()));
        addDetail(info, "PIX", p.getPix() == null || p.getPix().isBlank() ? "Não informado" : p.getPix());
        addDetail(info, "Tipo do PIX", p.getPixType() == null || p.getPixType().isBlank() ? "Não informado" : p.getPixType());
        addDetail(info, "Salário", formatMoney(p.getSalary()));
        addDetail(info, "Tipo", formatField(p.getType()));
        addDetail(info, "Status", p.isActive() ? "ATIVO" : "INATIVO");

        panel.add(header, BorderLayout.NORTH);
        panel.add(info, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(
                this,
                panel,
                "Detalhes do Promotor",
                JOptionPane.PLAIN_MESSAGE
        );
    }

    private void addDetail(JPanel panel, String label, String value) {
        JPanel item = new JPanel(new BorderLayout(0, 3));
        item.setBackground(Color.WHITE);
        item.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JLabel title = new JLabel(label);
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setForeground(TEXT_GRAY);

        JLabel content = new JLabel(value == null || value.isBlank() ? "Não informado" : value);
        content.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        content.setForeground(BLACK);

        item.add(title, BorderLayout.NORTH);
        item.add(content, BorderLayout.CENTER);

        panel.add(item);
    }

    private int getSelectedId() {
        int row = table.getSelectedRow();

        if (row == -1) {
            return -1;
        }

        int modelRow = table.convertRowIndexToModel(row);
        return (int) tableModel.getValueAt(modelRow, 0);
    }

    private String formatPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "";
        }

        String clean = phone.replaceAll("[^0-9]", "");

        if (clean.length() == 13 && clean.startsWith("55")) {
            return "+" + clean.substring(0, 2) +
                    " (" + clean.substring(2, 4) + ")" +
                    clean.substring(4, 9) +
                    "-" +
                    clean.substring(9);
        }

        if (clean.length() == 11) {
            return "+55 (" + clean.substring(0, 2) + ")" +
                    clean.substring(2, 7) +
                    "-" +
                    clean.substring(7);
        }

        return phone;
    }

    private String cleanPhone(String phone) {
        if (phone == null) {
            return "";
        }

        String clean = phone.replaceAll("[^0-9]", "");

        if (clean.startsWith("55") && clean.length() == 13) {
            return clean.substring(2);
        }

        return clean;
    }

    private String formatCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return "";
        }

        String clean = cpf.replaceAll("[^0-9]", "");

        if (clean.length() != 11) {
            return cpf;
        }

        return clean.substring(0, 3) + "."
                + clean.substring(3, 6) + "."
                + clean.substring(6, 9) + "-"
                + clean.substring(9);
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return moneyFormatter.format(BigDecimal.ZERO).replace('\u00A0', ' ');
        }

        return moneyFormatter.format(value.setScale(2, RoundingMode.HALF_UP))
                .replace('\u00A0', ' ');
    }

    private BigDecimal parseMoney(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Informe o salário.");
        }

        String normalized = value.trim()
                .replace("R$", "")
                .replace(" ", "")
                .replace("\u00A0", "");

        if (normalized.contains(",") && normalized.contains(".")) {
            if (normalized.lastIndexOf(",") > normalized.lastIndexOf(".")) {
                normalized = normalized.replace(".", "").replace(",", ".");
            } else {
                normalized = normalized.replace(",", "");
            }
        } else if (normalized.contains(",")) {
            normalized = normalized.replace(".", "").replace(",", ".");
        } else if (normalized.contains(".")) {
            int dotCount = normalized.length() - normalized.replace(".", "").length();
            int lastDot = normalized.lastIndexOf(".");
            int decimals = normalized.length() - lastDot - 1;

            if (dotCount > 1 || decimals == 3) {
                normalized = normalized.replace(".", "");
            }
        }

        BigDecimal amount = new BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O salário precisa ser maior que zero.");
        }

        return amount;
    }

    private String moneyForInput(BigDecimal value) {
        if (value == null) {
            return "";
        }

        return value.setScale(2, RoundingMode.HALF_UP)
                .toString()
                .replace(".", ",");
    }

    private String formatField(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value;
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }

        return date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private JTextField createTextField() {
        return createTextField("");
    }

    private JFormattedTextField createPhoneField() {
        try {
            javax.swing.text.MaskFormatter mask = new javax.swing.text.MaskFormatter("+55 (##)#####-####");
            mask.setPlaceholderCharacter(' ');
            JFormattedTextField field = new JFormattedTextField(mask);
            field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_GRAY),
                    BorderFactory.createEmptyBorder(5, 8, 5, 8)
            ));
            return field;
        } catch (Exception e) {
            return new JFormattedTextField();
        }
    }

    private JTextField createTextField(String text) {
        JTextField field = new JTextField(text);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        return field;
    }

    private JComboBox<String> createUfComboBox() {
        JComboBox<String> ufBox = new JComboBox<>(new String[]{
                "MA", "PI", "CE", "RN", "PB", "PE", "AL", "SE", "BA"
        });

        styleComboBox(ufBox);
        return ufBox;
    }

    private JComboBox<String> createCompanyLinkComboBox() {
        JComboBox<String> companyBox = new JComboBox<>(new String[]{"AT", "TEJO"});
        styleComboBox(companyBox);
        return companyBox;
    }

    private void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        comboBox.setBackground(WHITE);
        comboBox.setForeground(BLACK);
    }

    private JButton createPrimaryButton(String text) {
        JButton button = baseButton(text);
        button.setBackground(ORANGE);
        button.setForeground(WHITE);
        button.setBorderPainted(false);
        return button;
    }

    private JButton createDarkButton(String text) {
        JButton button = baseButton(text);
        button.setBackground(BLACK);
        button.setForeground(WHITE);
        button.setBorderPainted(false);
        return button;
    }

    private JButton createSecondaryButton(String text) {
        JButton button = baseButton(text);
        button.setBackground(WHITE);
        button.setForeground(BLACK);
        button.setBorder(BorderFactory.createLineBorder(BORDER_GRAY));
        return button;
    }

    private JButton createDangerButton(String text) {
        JButton button = baseButton(text);
        button.setBackground(RED);
        button.setForeground(WHITE);
        button.setBorderPainted(false);
        return button;
    }

    private JButton baseButton(String text) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(130, 38));
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Atenção", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    private static class PromoterItem {
        private final int id;
        private final String name;

        public PromoterItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String toString() {
            return name;
        }
    }

    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {

        public ButtonRenderer() {
            setText("Editar");
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setFocusPainted(false);
            setBorderPainted(false);
            setBackground(WHITE);
            setForeground(BLACK);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        public Component getTableCellRendererComponent(
                JTable t,
                Object v,
                boolean s,
                boolean f,
                int r,
                int c
        ) {
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {

        private final JButton button;
        private int row;
        private final PromoterFrame frame;

        public ButtonEditor(JCheckBox checkBox, PromoterFrame frame) {
            super(checkBox);
            this.frame = frame;

            button = new JButton("Editar");
            button.setFont(new Font("Segoe UI", Font.BOLD, 12));
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.setBackground(ORANGE);
            button.setForeground(WHITE);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));

            button.addActionListener(e -> {
                int modelRow = table.convertRowIndexToModel(row);
                int id = (int) tableModel.getValueAt(modelRow, 0);
                fireEditingStopped();
                frame.openEditDialog(id);
            });
        }

        public Component getTableCellEditorComponent(
                JTable t,
                Object v,
                boolean s,
                int r,
                int c
        ) {
            row = r;
            return button;
        }

        public Object getCellEditorValue() {
            return "Editar";
        }
    }
}