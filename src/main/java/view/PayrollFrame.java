package view;

import controller.PayrollController;
import controller.PromoterController;
import model.PayrollLine;
import model.PromoterPaymentData;
import model.Promoter;
import model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PayrollFrame extends JFrame {

    private final PayrollController payrollController = new PayrollController();
    private final PromoterController promoterController = new PromoterController();

    private final User loggedUser;
    private final boolean canManageDiscounts;

    private final Locale BR_LOCALE = new Locale("pt", "BR");
    private final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(BR_LOCALE);

    private final Color ORANGE = new Color(255, 102, 0);
    private final Color BLACK = new Color(18, 18, 18);
    private final Color WHITE = Color.WHITE;
    private final Color LIGHT_GRAY = new Color(245, 245, 245);
    private final Color BORDER_GRAY = new Color(220, 220, 220);
    private final Color TEXT_GRAY = new Color(90, 90, 90);

    private JSpinner startSpinner;
    private JSpinner endSpinner;
    private JComboBox<String> typeBox;

    private JTable payrollTable;
    private DefaultTableModel tableModel;

    private JLabel lblTotalBase;
    private JLabel lblTotalDiscounts;
    private JLabel lblTotalNet;
    private JLabel lblPromoters;

    private List<PayrollLine> currentLines = new ArrayList<>();
    private LocalDate currentStartDate;
    private LocalDate currentEndDate;
    private String currentPromoterType = "TODOS";

    public PayrollFrame() {
        this(null);
    }

    public PayrollFrame(User loggedUser) {
        this.loggedUser = loggedUser;
        this.canManageDiscounts = loggedUser != null
                && loggedUser.getJobTittle() != null
                && loggedUser.getJobTittle().equalsIgnoreCase("RH");

        setTitle("Sistema At Promo - Folha de Pagamento");
        setSize(1120, 850);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createMainPanel(), BorderLayout.CENTER);

        generatePayroll();

        setVisible(true);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(null);
        header.setPreferredSize(new Dimension(1120, 95));
        header.setBackground(BLACK);

        JLabel title = new JLabel("Folha de Pagamento");
        title.setForeground(WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setBounds(30, 18, 430, 36);
        header.add(title);

        JLabel subtitle = new JLabel("Conferência profissional de salário base, descontos e líquido a pagar por promotor.");
        subtitle.setForeground(new Color(210, 210, 210));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setBounds(32, 56, 760, 22);
        header.add(subtitle);

        JPanel line = new JPanel();
        line.setBackground(ORANGE);
        line.setBounds(30, 84, 1040, 4);
        header.add(line);

        return header;
    }

    private JPanel createMainPanel() {
        JPanel main = new JPanel(null);
        main.setBackground(LIGHT_GRAY);

        createFilterPanel(main);
        createDashboard(main);
        createTable(main);
        createActionButtons(main);

        return main;
    }

    private void createFilterPanel(JPanel panel) {
        JPanel filterPanel = new JPanel(null);
        filterPanel.setBackground(WHITE);
        filterPanel.setBorder(BorderFactory.createLineBorder(BORDER_GRAY));
        filterPanel.setBounds(30, 25, 1040, 105);
        panel.add(filterPanel);

        JLabel filterTitle = createSectionTitle("Filtros da folha");
        filterTitle.setBounds(20, 12, 220, 24);
        filterPanel.add(filterTitle);

        JLabel startLabel = createFieldLabel("Data início");
        startLabel.setBounds(20, 45, 100, 20);
        filterPanel.add(startLabel);

        startSpinner = new JSpinner(new SpinnerDateModel());
        startSpinner.setEditor(new JSpinner.DateEditor(startSpinner, "dd/MM/yyyy"));
        startSpinner.setValue(toDate(LocalDate.now().withDayOfMonth(1)));
        startSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        startSpinner.setBounds(20, 67, 135, 32);
        filterPanel.add(startSpinner);

        JLabel endLabel = createFieldLabel("Data fim");
        endLabel.setBounds(175, 45, 100, 20);
        filterPanel.add(endLabel);

        endSpinner = new JSpinner(new SpinnerDateModel());
        endSpinner.setEditor(new JSpinner.DateEditor(endSpinner, "dd/MM/yyyy"));
        endSpinner.setValue(toDate(LocalDate.now()));
        endSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        endSpinner.setBounds(175, 67, 135, 32);
        filterPanel.add(endSpinner);

        JLabel typeLabel = createFieldLabel("Tipo de promotor");
        typeLabel.setBounds(330, 45, 140, 20);
        filterPanel.add(typeLabel);

        typeBox = new JComboBox<>(new String[]{"TODOS", "CLT", "MEI", "FERISTA"});
        typeBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        typeBox.setBounds(330, 67, 145, 32);
        filterPanel.add(typeBox);

        JButton btnGenerate = createPrimaryButton("Gerar Folha");
        btnGenerate.setBounds(510, 64, 135, 36);
        btnGenerate.addActionListener(e -> generatePayroll());
        filterPanel.add(btnGenerate);

        JButton btnClear = createDarkButton("Limpar");
        btnClear.setBounds(655, 64, 95, 36);
        btnClear.addActionListener(e -> clearPayroll());
        filterPanel.add(btnClear);

        JButton btnExportReport = createDarkButton("Exportar Relatório");
        btnExportReport.setBounds(765, 64, 160, 36);
        btnExportReport.addActionListener(e -> exportPayrollReport());
        filterPanel.add(btnExportReport);
    }

    private void createDashboard(JPanel panel) {
        JPanel dashboard = new JPanel(null);
        dashboard.setBackground(WHITE);
        dashboard.setBorder(BorderFactory.createLineBorder(BORDER_GRAY));
        dashboard.setBounds(30, 145, 1040, 82);
        panel.add(dashboard);

        lblTotalBase = createDashboardLabel("Base: R$ 0,00", 25);
        lblTotalDiscounts = createDashboardLabel("Descontos: R$ 0,00", 285);
        lblTotalNet = createDashboardLabel("Líquido: R$ 0,00", 545);
        lblPromoters = createDashboardLabel("Promotores: 0", 805);

        dashboard.add(lblTotalBase);
        dashboard.add(lblTotalDiscounts);
        dashboard.add(lblTotalNet);
        dashboard.add(lblPromoters);
    }

    private JLabel createDashboardLabel(String text, int x) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 15));
        label.setForeground(BLACK);
        label.setBounds(x, 25, 235, 30);
        return label;
    }

    private void createTable(JPanel panel) {
        tableModel = new DefaultTableModel(
                new Object[]{
                        "ID", "Promotor", "Tipo", "Salário/Base",
                        "Descontos", "Líquido", "Status", "Observação"
                }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        payrollTable = new JTable(tableModel);
        payrollTable.setRowHeight(31);
        payrollTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        payrollTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        payrollTable.setGridColor(new Color(235, 235, 235));
        payrollTable.setShowVerticalLines(false);

        payrollTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
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

                    if (column == 3 || column == 4 || column == 5) {
                        label.setHorizontalAlignment(SwingConstants.RIGHT);
                    } else {
                        label.setHorizontalAlignment(SwingConstants.LEFT);
                    }
                }

                String status = table.getValueAt(row, 6).toString();

                if (isSelected) {
                    component.setBackground(new Color(210, 230, 255));
                    component.setForeground(Color.BLACK);
                    return component;
                }

                if ("ATENÇÃO".equalsIgnoreCase(status)) {
                    component.setBackground(new Color(255, 235, 230));
                } else {
                    component.setBackground(Color.WHITE);
                }

                component.setForeground(Color.BLACK);
                return component;
            }
        });

        JTableHeader header = payrollTable.getTableHeader();
        header.setBackground(BLACK);
        header.setForeground(WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JScrollPane scrollPane = new JScrollPane(payrollTable);
        scrollPane.setBounds(30, 245, 1040, 360);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_GRAY));
        scrollPane.getViewport().setBackground(WHITE);
        panel.add(scrollPane);

        payrollTable.getColumnModel().getColumn(0).setMinWidth(0);
        payrollTable.getColumnModel().getColumn(0).setMaxWidth(0);
        payrollTable.getColumnModel().getColumn(0).setWidth(0);

        payrollTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        payrollTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        payrollTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        payrollTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        payrollTable.getColumnModel().getColumn(5).setPreferredWidth(120);
        payrollTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        payrollTable.getColumnModel().getColumn(7).setPreferredWidth(270);
    }

    private void createActionButtons(JPanel panel) {
        if (canManageDiscounts) {
            JButton btnDiscounts = createDarkButton("Descontos");
            btnDiscounts.setBounds(500, 625, 135, 38);
            btnDiscounts.addActionListener(e -> openDiscountsDialog());
            panel.add(btnDiscounts);
        }

        JButton btnRefresh = createPrimaryButton("Atualizar");
        btnRefresh.setBounds(650, 625, 125, 38);
        btnRefresh.addActionListener(e -> generatePayroll());
        panel.add(btnRefresh);

        JButton btnPixMei = createPrimaryButton("Gerar PIX MEI");
        btnPixMei.setBounds(790, 625, 140, 38);
        btnPixMei.addActionListener(e -> exportMeiPixBatch());
        panel.add(btnPixMei);

        JButton btnClose = createDarkButton("Fechar");
        btnClose.setBounds(945, 625, 125, 38);
        btnClose.addActionListener(e -> dispose());
        panel.add(btnClose);
    }

    private void openDiscountsDialog() {
        if (!canManageDiscounts) {
            JOptionPane.showMessageDialog(
                    this,
                    "Apenas o RH pode gerenciar descontos da folha.",
                    "Acesso negado",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            LocalDate start = getStartDate();
            LocalDate end = getEndDate();

            if (start.isAfter(end)) {
                JOptionPane.showMessageDialog(
                        this,
                        "Data inicial não pode ser maior que a final.",
                        "Período inválido",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            JDialog dialog = new JDialog(this, "Descontos da Folha", true);
            dialog.setSize(820, 520);
            dialog.setLocationRelativeTo(this);
            dialog.setResizable(false);
            dialog.setLayout(new BorderLayout());
            dialog.getContentPane().setBackground(WHITE);

            JPanel header = new JPanel(null);
            header.setPreferredSize(new Dimension(820, 85));
            header.setBackground(BLACK);

            JLabel title = new JLabel("Descontos da Folha");
            title.setForeground(WHITE);
            title.setFont(new Font("Segoe UI", Font.BOLD, 22));
            title.setBounds(24, 14, 350, 30);
            header.add(title);

            JLabel subtitle = new JLabel("Lançamentos que serão abatidos do salário/base no período selecionado.");
            subtitle.setForeground(new Color(210, 210, 210));
            subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            subtitle.setBounds(26, 47, 620, 22);
            header.add(subtitle);

            JPanel line = new JPanel();
            line.setBackground(ORANGE);
            line.setBounds(24, 75, 760, 3);
            header.add(line);

            dialog.add(header, BorderLayout.NORTH);

            DefaultTableModel discountModel = new DefaultTableModel(
                    new Object[]{"ID", "Promotor", "Valor", "Data", "Motivo"}, 0
            ) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            JTable discountTable = new JTable(discountModel);
            discountTable.setRowHeight(30);
            discountTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            discountTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
            discountTable.getTableHeader().setBackground(BLACK);
            discountTable.getTableHeader().setForeground(WHITE);
            discountTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            discountTable.getColumnModel().getColumn(0).setMinWidth(0);
            discountTable.getColumnModel().getColumn(0).setMaxWidth(0);
            discountTable.getColumnModel().getColumn(0).setWidth(0);

            discountTable.getColumnModel().getColumn(1).setPreferredWidth(230);
            discountTable.getColumnModel().getColumn(2).setPreferredWidth(100);
            discountTable.getColumnModel().getColumn(3).setPreferredWidth(95);
            discountTable.getColumnModel().getColumn(4).setPreferredWidth(330);

            JPanel center = new JPanel(new BorderLayout());
            center.setBackground(WHITE);
            center.setBorder(BorderFactory.createEmptyBorder(18, 22, 12, 22));

            JScrollPane scrollPane = new JScrollPane(discountTable);
            scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_GRAY));
            center.add(scrollPane, BorderLayout.CENTER);

            dialog.add(center, BorderLayout.CENTER);

            JPanel footer = new JPanel(null);
            footer.setPreferredSize(new Dimension(820, 75));
            footer.setBackground(LIGHT_GRAY);

            JButton btnNew = createPrimaryButton("Novo");
            btnNew.setBounds(250, 18, 100, 36);
            btnNew.addActionListener(e -> {
                openDiscountForm(null, discountModel, start, end);
                refreshDiscountTable(discountModel, start, end);
                generatePayroll();
            });
            footer.add(btnNew);

            JButton btnEdit = createDarkButton("Editar");
            btnEdit.setBounds(365, 18, 100, 36);
            btnEdit.addActionListener(e -> {
                int row = discountTable.getSelectedRow();

                if (row == -1) {
                    JOptionPane.showMessageDialog(dialog, "Selecione um desconto para editar.");
                    return;
                }

                int modelRow = discountTable.convertRowIndexToModel(row);
                openDiscountForm(modelRow, discountModel, start, end);
                refreshDiscountTable(discountModel, start, end);
                generatePayroll();
            });
            footer.add(btnEdit);

            JButton btnDelete = createDarkButton("Excluir");
            btnDelete.setBounds(480, 18, 100, 36);
            btnDelete.addActionListener(e -> {
                int row = discountTable.getSelectedRow();

                if (row == -1) {
                    JOptionPane.showMessageDialog(dialog, "Selecione um desconto para excluir.");
                    return;
                }

                int confirm = JOptionPane.showConfirmDialog(
                        dialog,
                        "Deseja excluir este desconto?",
                        "Confirmar exclusão",
                        JOptionPane.YES_NO_OPTION
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    int modelRow = discountTable.convertRowIndexToModel(row);
                    int id = (int) discountModel.getValueAt(modelRow, 0);

                    payrollController.deleteDiscount(id);
                    refreshDiscountTable(discountModel, start, end);
                    generatePayroll();
                }
            });
            footer.add(btnDelete);

            JButton btnClose = createDarkButton("Fechar");
            btnClose.setBounds(595, 18, 100, 36);
            btnClose.addActionListener(e -> dialog.dispose());
            footer.add(btnClose);

            dialog.add(footer, BorderLayout.SOUTH);

            refreshDiscountTable(discountModel, start, end);

            dialog.setVisible(true);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshDiscountTable(DefaultTableModel discountModel, LocalDate start, LocalDate end) {
        discountModel.setRowCount(0);

        List<String> discounts = payrollController.listDiscounts(start, end);

        for (String line : discounts) {
            String[] parts = line.split("\\|", 5);

            if (parts.length < 5) {
                continue;
            }

            discountModel.addRow(new Object[]{
                    Integer.parseInt(parts[0].trim()),
                    parts[1].trim(),
                    parts[2].trim(),
                    parts[3].trim(),
                    parts[4].trim()
            });
        }
    }

    private void openDiscountForm(Integer selectedRow, DefaultTableModel discountModel, LocalDate start, LocalDate end) {
        try {
            JTextField searchField = new JTextField();
            DefaultListModel<PromoterItem> listModel = new DefaultListModel<>();
            JList<PromoterItem> promoterList = new JList<>(listModel);

            promoterList.setVisibleRowCount(5);
            promoterList.setFont(new Font("Segoe UI", Font.PLAIN, 13));

            JScrollPane promoterScroll = new JScrollPane(promoterList);
            promoterScroll.setPreferredSize(new Dimension(320, 90));

            searchField.addCaretListener(e -> {
                String text = searchField.getText().trim();
                listModel.clear();

                if (text.length() < 2) {
                    return;
                }

                List<Promoter> promoters = promoterController.searchByNameIncludingInactive(text);

                for (Promoter promoter : promoters) {
                    String status = promoter.isActive() ? "ATIVO" : "INATIVO";
                    listModel.addElement(new PromoterItem(promoter.getId(), promoter.getName() + " - " + status));
                }
            });

            JTextField txtAmount = new JTextField();
            JTextArea txtDescription = new JTextArea(4, 22);
            txtDescription.setLineWrap(true);
            txtDescription.setWrapStyleWord(true);

            JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
            dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy"));
            dateSpinner.setValue(toDate(LocalDate.now()));

            boolean editing = selectedRow != null;

            if (editing) {
                txtAmount.setText(discountModel.getValueAt(selectedRow, 2).toString().replace("R$", "").trim());
                txtDescription.setText(discountModel.getValueAt(selectedRow, 4).toString());
            }

            Object[] fields;

            if (editing) {
                fields = new Object[]{
                        "Valor do desconto:", txtAmount,
                        "Data:", dateSpinner,
                        "Motivo:", new JScrollPane(txtDescription)
                };
            } else {
                fields = new Object[]{
                        "Buscar promotor:", searchField,
                        "Resultados:", promoterScroll,
                        "Valor do desconto:", txtAmount,
                        "Data:", dateSpinner,
                        "Motivo:", new JScrollPane(txtDescription)
                };
            }

            int option = JOptionPane.showConfirmDialog(
                    this,
                    fields,
                    editing ? "Editar Desconto" : "Novo Desconto",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (option != JOptionPane.OK_OPTION) {
                return;
            }

            BigDecimal amount = parseMoney(txtAmount.getText());
            LocalDate date = ((Date) dateSpinner.getValue())
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            String description = txtDescription.getText().trim();

            if (editing) {
                int id = (int) discountModel.getValueAt(selectedRow, 0);
                payrollController.updateDiscount(id, amount, date, description);
                JOptionPane.showMessageDialog(this, "Desconto atualizado com sucesso.");
            } else {
                PromoterItem selectedPromoter = promoterList.getSelectedValue();

                if (selectedPromoter == null) {
                    JOptionPane.showMessageDialog(this, "Selecione um promotor.");
                    return;
                }

                payrollController.registerDiscount(
                        selectedPromoter.getId(),
                        amount,
                        date,
                        description
                );

                JOptionPane.showMessageDialog(this, "Desconto cadastrado com sucesso.");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
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
            BigDecimal amount = new BigDecimal(cleanValue).setScale(2, RoundingMode.HALF_UP);

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("O valor precisa ser maior que zero.");
            }

            return amount;

        } catch (NumberFormatException e) {
            throw new RuntimeException("Valor inválido. Use o formato 1300,00 ou 1.300,00.");
        }
    }

    private void generatePayroll() {
        try {
            LocalDate start = getStartDate();
            LocalDate end = getEndDate();

            if (start.isAfter(end)) {
                JOptionPane.showMessageDialog(
                        this,
                        "Data inicial não pode ser maior que a final.",
                        "Período inválido",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            String type = typeBox.getSelectedItem().toString();

            currentLines = payrollController.generatePayrollLines(start, end, type);
            currentStartDate = start;
            currentEndDate = end;
            currentPromoterType = type;

            fillTable(currentLines);
            updateDashboard(currentLines);

            if (currentLines.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Nenhum promotor encontrado para esse filtro.",
                        "Atenção",
                        JOptionPane.WARNING_MESSAGE
                );
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void fillTable(List<PayrollLine> lines) {
        tableModel.setRowCount(0);

        for (PayrollLine line : lines) {
            tableModel.addRow(new Object[]{
                    line.getPromoterId(),
                    line.getPromoterName(),
                    line.getPromoterType(),
                    formatMoney(line.getBaseSalary()),
                    formatMoney(line.getDiscounts()),
                    formatMoney(line.getNetAmount()),
                    line.getStatus(),
                    line.getObservation()
            });
        }
    }

    private void updateDashboard(List<PayrollLine> lines) {
        BigDecimal totalBase = BigDecimal.ZERO;
        BigDecimal totalDiscounts = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        int attentionCount = 0;

        for (PayrollLine line : lines) {
            totalBase = totalBase.add(nullToZero(line.getBaseSalary()));
            totalDiscounts = totalDiscounts.add(nullToZero(line.getDiscounts()));
            totalNet = totalNet.add(nullToZero(line.getNetAmount()));

            if ("ATENÇÃO".equalsIgnoreCase(line.getStatus())) {
                attentionCount++;
            }
        }

        lblTotalBase.setText("Base: " + formatMoney(totalBase));
        lblTotalDiscounts.setText("Descontos: " + formatMoney(totalDiscounts));
        lblTotalNet.setText("Líquido: " + formatMoney(totalNet));

        if (attentionCount > 0) {
            lblPromoters.setText("Promotores: " + lines.size() + " | Atenção: " + attentionCount);
            lblPromoters.setForeground(new Color(180, 40, 40));
        } else {
            lblPromoters.setText("Promotores: " + lines.size());
            lblPromoters.setForeground(BLACK);
        }
    }

    private void clearPayroll() {
        currentLines = new ArrayList<>();
        currentStartDate = null;
        currentEndDate = null;
        currentPromoterType = "TODOS";
        tableModel.setRowCount(0);
        updateDashboard(currentLines);
    }

    private void exportPayrollReport() {
        try {
            if (currentLines == null || currentLines.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Gere a folha antes de exportar o relatório.",
                        "Atenção",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            String path = util.FileSaveDialog.chooseXlsxPath(this, "folha_pagamento.xlsx");

            if (path == null) {
                return;
            }

            util.ExcelGenerator.generatePayrollReport(
                    currentLines,
                    currentStartDate,
                    currentEndDate,
                    currentPromoterType,
                    path
            );

            JOptionPane.showMessageDialog(this, "Relatório da folha exportado com sucesso.");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportMeiPixBatch() {
        try {
            JSpinner paymentDateSpinner = new JSpinner(new SpinnerDateModel());
            paymentDateSpinner.setEditor(new JSpinner.DateEditor(paymentDateSpinner, "dd/MM/yyyy"));
            paymentDateSpinner.setValue(toDate(LocalDate.now()));

            int result = JOptionPane.showConfirmDialog(
                    this,
                    paymentDateSpinner,
                    "Data de pagamento do PIX lote MEI",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            LocalDate paymentDate = ((Date) paymentDateSpinner.getValue())
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            List<PromoterPaymentData> payments = payrollController.getMeiPixBatch(paymentDate);

            if (payments.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Nenhum MEI ativo com chave PIX encontrado para gerar o PIX lote.",
                        "Atenção",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            String path = util.FileSaveDialog.chooseXlsxPath(this, "pix_lote_mei.xlsx");

            if (path == null) {
                return;
            }

            util.ExcelGenerator.generatePixBatch(payments, path);

            JOptionPane.showMessageDialog(this, "PIX lote MEI gerado com sucesso.");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private LocalDate getStartDate() {
        Date date = (Date) startSpinner.getValue();

        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    private LocalDate getEndDate() {
        Date date = (Date) endSpinner.getValue();

        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "R$ 0,00";
        }

        return MONEY_FORMAT.format(value.setScale(2, RoundingMode.HALF_UP))
                .replace('\u00A0', ' ');
    }

    private JLabel createSectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(BLACK);
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));
        return label;
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_GRAY);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        return label;
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

    private JButton baseButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
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

        @Override
        public String toString() {
            return name;
        }
    }
}