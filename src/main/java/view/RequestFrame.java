package view;

import controller.PromoterController;
import controller.RequestController;
import model.Promoter;
import model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RequestFrame extends JFrame {

    private final RequestController requestController;
    private final PromoterController promoterController;
    private final User loggedUser;

    private JTable table;
    private DefaultTableModel tableModel;

    private JLabel lblTotal;
    private JLabel lblPending;
    private JLabel lblApproved;
    private JLabel lblRejected;
    private JLabel lblCurrentView;

    private String currentMode = "PENDENTES";
    private LocalDateTime currentStart;
    private LocalDateTime currentEnd;

    private final Color ORANGE = new Color(255, 102, 0);
    private final Color BLACK = new Color(18, 18, 18);
    private final Color WHITE = Color.WHITE;
    private final Color LIGHT_GRAY = new Color(245, 245, 245);
    private final Color BORDER_GRAY = new Color(220, 220, 220);
    private final Color TEXT_GRAY = new Color(90, 90, 90);
    private final Color RED = new Color(190, 40, 40);
    private final Color GREEN = new Color(40, 130, 70);
    private final Color YELLOW_BG = new Color(255, 249, 225);
    private final Color GREEN_BG = new Color(225, 245, 232);
    private final Color RED_BG = new Color(250, 228, 228);

    private final Locale brLocale = new Locale("pt", "BR");
    private final NumberFormat moneyFormatter = NumberFormat.getCurrencyInstance(brLocale);

    public RequestFrame(User loggedUser) {
        this.loggedUser = loggedUser;
        this.requestController = new RequestController();
        this.promoterController = new PromoterController();

        setTitle("Sistema At Promo - Solicitações");
        setSize(1360, 760);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createMainPanel(), BorderLayout.CENTER);

        loadPending();

        setVisible(true);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(null);
        header.setPreferredSize(new Dimension(1360, 105));
        header.setBackground(BLACK);

        JLabel title = new JLabel("Gerenciamento de Solicitações");
        title.setForeground(WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 27));
        title.setBounds(32, 20, 520, 34);
        header.add(title);

        JLabel subtitle = new JLabel("Acompanhe solicitações do RH, aprovações financeiras, vínculo da empresa e dados para pagamento.");
        subtitle.setForeground(new Color(210, 210, 210));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setBounds(34, 58, 780, 22);
        header.add(subtitle);

        JLabel userLabel = new JLabel(loggedUser.getName() + " - " + loggedUser.getJobTittle());
        userLabel.setForeground(ORANGE);
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        userLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        userLabel.setBounds(910, 30, 390, 30);
        header.add(userLabel);

        JPanel line = new JPanel();
        line.setBackground(ORANGE);
        line.setBounds(32, 88, 250, 4);
        header.add(line);

        return header;
    }

    private JPanel createMainPanel() {
        JPanel main = new JPanel(new BorderLayout(0, 14));
        main.setBackground(LIGHT_GRAY);
        main.setBorder(BorderFactory.createEmptyBorder(22, 26, 24, 26));

        JPanel topPanel = new JPanel(new BorderLayout(0, 12));
        topPanel.setBackground(LIGHT_GRAY);
        topPanel.add(createSummaryPanel(), BorderLayout.NORTH);
        topPanel.add(createActionPanel(), BorderLayout.SOUTH);

        main.add(topPanel, BorderLayout.NORTH);
        main.add(createTablePanel(), BorderLayout.CENTER);
        main.add(createFooterPanel(), BorderLayout.SOUTH);

        return main;
    }

    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 12, 0));
        panel.setBackground(LIGHT_GRAY);

        lblTotal = createSummaryCard(panel, "Total exibido", "0", BLACK);
        lblPending = createSummaryCard(panel, "Pendentes", "0", ORANGE);
        lblApproved = createSummaryCard(panel, "Aprovadas", "0", GREEN);
        lblRejected = createSummaryCard(panel, "Rejeitadas", "0", RED);

        return panel;
    }

    private JLabel createSummaryCard(JPanel parent, String title, String value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(TEXT_GRAY);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(accent);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        parent.add(card);
        return valueLabel;
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
        panel.setBackground(WHITE);
        panel.setPreferredSize(new Dimension(1200, 65));
        panel.setBorder(BorderFactory.createLineBorder(BORDER_GRAY));

        JButton btnPending = createPrimaryButton("Pendentes", 125);
        JButton btnListAll = createSecondaryButton("Todas", 105);
        JButton btnPeriod = createSecondaryButton("Período", 110);
        JButton btnCreate = createPrimaryButton("Criar Solicitação", 150);

        panel.add(btnPending);
        panel.add(btnListAll);
        panel.add(btnPeriod);
        panel.add(btnCreate);

        btnPending.addActionListener(e -> loadPending());
        btnListAll.addActionListener(e -> loadAll());
        btnPeriod.addActionListener(e -> loadByPeriod());
        btnCreate.addActionListener(e -> openCreateDialog());

        if (isFinanceUser()) {
            btnCreate.setEnabled(false);
        }

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(WHITE);
        panel.setBorder(BorderFactory.createLineBorder(BORDER_GRAY));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel title = new JLabel("Solicitações");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(BLACK);

        lblCurrentView = new JLabel("Pendentes");
        lblCurrentView.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCurrentView.setForeground(TEXT_GRAY);
        lblCurrentView.setHorizontalAlignment(SwingConstants.RIGHT);

        header.add(title, BorderLayout.WEST);
        header.add(lblCurrentView, BorderLayout.EAST);

        panel.add(header, BorderLayout.NORTH);

        String[] columns = {
                "ID", "Selecionar", "Promotor", "Vínculo", "Tipo",
                "Valor", "Mensagem", "Status", "Data", "MensagemCompleta"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1) {
                    return Boolean.class;
                }

                return Object.class;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(34);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setSelectionBackground(new Color(255, 225, 205));
        table.setSelectionForeground(BLACK);
        table.setGridColor(new Color(235, 235, 235));
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);

        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(BLACK);
        table.getTableHeader().setForeground(WHITE);
        table.getTableHeader().setReorderingAllowed(false);

        table.setDefaultRenderer(Object.class, new RequestTableRenderer());

        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);

        table.getColumnModel().getColumn(1).setPreferredWidth(78);
        table.getColumnModel().getColumn(2).setPreferredWidth(230);
        table.getColumnModel().getColumn(3).setPreferredWidth(78);
        table.getColumnModel().getColumn(4).setPreferredWidth(150);
        table.getColumnModel().getColumn(5).setPreferredWidth(110);
        table.getColumnModel().getColumn(6).setPreferredWidth(310);
        table.getColumnModel().getColumn(7).setPreferredWidth(95);
        table.getColumnModel().getColumn(8).setPreferredWidth(135);

        table.getColumnModel().getColumn(9).setMinWidth(0);
        table.getColumnModel().getColumn(9).setMaxWidth(0);
        table.getColumnModel().getColumn(9).setWidth(0);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showRequestDetails();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_GRAY));
        scrollPane.getViewport().setBackground(WHITE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        JLabel text = new JLabel("Marque várias solicitações para aprovar em lote.");
        text.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        text.setForeground(TEXT_GRAY);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setBackground(WHITE);

        JButton btnApprove = createPrimaryButton("Aprovar Seleção", 150);
        JButton btnReject = createDangerButton("Rejeitar", 110);
        JButton btnReopen = createDarkButton("Reabrir", 105);
        JButton btnDetails = createDarkButton("Detalhes", 105);
        JButton btnExportPixBatch = createPrimaryButton("Exportar Pix", 135);
        JButton btnExportPending = createDarkButton("Exportar Pendentes", 170);

        actions.add(btnApprove);
        actions.add(btnReject);
        actions.add(btnReopen);
        actions.add(btnDetails);
        actions.add(btnExportPixBatch);
        actions.add(btnExportPending);

        panel.add(text, BorderLayout.WEST);
        panel.add(actions, BorderLayout.EAST);

        btnApprove.addActionListener(e -> approveSelected());
        btnReject.addActionListener(e -> rejectSelected());
        btnReopen.addActionListener(e -> reopenSelected());
        btnDetails.addActionListener(e -> showRequestDetails());
        btnExportPixBatch.addActionListener(e -> exportPixBatch());
        btnExportPending.addActionListener(e -> exportPendingRequests());

        if (isRhUser()) {
            btnApprove.setEnabled(false);
            btnReject.setEnabled(false);
            btnReopen.setEnabled(false);
        }

        return panel;
    }

    private void loadAll() {
        currentMode = "TODAS";
        fillTable(requestController.getAllWithPromoterName());
        lblCurrentView.setText("Exibindo todas as solicitações");
    }

    private void loadPending() {
        currentMode = "PENDENTES";
        fillTable(requestController.getPendingWithPromoterName());
        lblCurrentView.setText("Exibindo solicitações pendentes");
    }

    private void loadByPeriod() {
        try {
            JSpinner startSpinner = new JSpinner(new SpinnerDateModel());
            JSpinner endSpinner = new JSpinner(new SpinnerDateModel());

            startSpinner.setEditor(new JSpinner.DateEditor(startSpinner, "dd/MM/yyyy"));
            endSpinner.setEditor(new JSpinner.DateEditor(endSpinner, "dd/MM/yyyy"));

            JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
            panel.add(new JLabel("Data início:"));
            panel.add(startSpinner);
            panel.add(new JLabel("Data fim:"));
            panel.add(endSpinner);

            int option = JOptionPane.showConfirmDialog(
                    this,
                    panel,
                    "Selecionar Período",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (option != JOptionPane.OK_OPTION) {
                return;
            }

            java.util.Date startDate = (java.util.Date) startSpinner.getValue();
            java.util.Date endDate = (java.util.Date) endSpinner.getValue();

            LocalDate start = startDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            LocalDate end = endDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            if (start.isAfter(end)) {
                showWarning("Data inicial não pode ser maior que a final.");
                return;
            }

            currentMode = "PERIODO";
            currentStart = start.atStartOfDay();
            currentEnd = end.atTime(23, 59, 59);

            fillTable(requestController.getByPeriodWithPromoterName(currentStart, currentEnd));
            lblCurrentView.setText("Período: " + formatDate(start) + " até " + formatDate(end));

        } catch (Exception e) {
            showError("Erro ao selecionar período.");
        }
    }

    private void refreshCurrentView() {
        if ("TODAS".equals(currentMode)) {
            loadAll();
            return;
        }

        if ("PERIODO".equals(currentMode) && currentStart != null && currentEnd != null) {
            fillTable(requestController.getByPeriodWithPromoterName(currentStart, currentEnd));
            lblCurrentView.setText("Período selecionado");
            return;
        }

        loadPending();
    }

    private void openCreateDialog() {
        JTextField searchField = createInputField();

        DefaultListModel<PromoterItem> listModel = new DefaultListModel<>();
        JList<PromoterItem> promoterList = new JList<>(listModel);
        promoterList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        promoterList.setSelectionBackground(new Color(255, 225, 205));
        promoterList.setSelectionForeground(BLACK);

        JScrollPane listScroll = new JScrollPane(promoterList);
        listScroll.setPreferredSize(new Dimension(440, 115));

        searchField.addCaretListener(e -> {
            String text = searchField.getText().trim();
            listModel.clear();

            if (text.length() < 2) {
                return;
            }

            List<Promoter> promoters = promoterController.searchByNameIncludingInactive(text);

            for (Promoter p : promoters) {
                String status = p.isActive() ? "ATIVO" : "INATIVO";
                String company = p.getCompanyLink() == null || p.getCompanyLink().isBlank()
                        ? "Sem vínculo"
                        : p.getCompanyLink();

                listModel.addElement(new PromoterItem(
                        p.getId(),
                        p.getName() + " - " + company + " - " + status
                ));
            }
        });

        JComboBox<String> typeBox = new JComboBox<>(
                requestController.getValidTypeLabels().toArray(new String[0])
        );
        typeBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JTextField amountField = createInputField();

        JTextArea messageArea = new JTextArea(5, 30);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        messageArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.setPreferredSize(new Dimension(480, 430));
        panel.add(createDialogLabel("Buscar promotor pelo nome"));
        panel.add(searchField);
        panel.add(createDialogLabel("Resultados"));
        panel.add(listScroll);
        panel.add(createDialogLabel("Tipo"));
        panel.add(typeBox);
        panel.add(createDialogLabel("Valor"));
        panel.add(amountField);
        panel.add(createDialogLabel("Mensagem / PIX / Dados para pagamento"));
        panel.add(new JScrollPane(messageArea));

        int option = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Criar Solicitação",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (option == JOptionPane.OK_OPTION) {
            try {
                PromoterItem selectedPromoter = promoterList.getSelectedValue();

                if (selectedPromoter == null) {
                    showWarning("Selecione um promotor na lista.");
                    return;
                }

                BigDecimal amount = parseMoney(amountField.getText());
                String selectedType = typeBox.getSelectedItem().toString();
                String type = requestController.toInternalType(selectedType);
                String message = messageArea.getText().trim();

                if (message.isBlank()) {
                    showWarning("Mensagem não pode ficar vazia.");
                    return;
                }

                requestController.createRequest(
                        loggedUser.getId(),
                        0,
                        selectedPromoter.getId(),
                        type,
                        amount,
                        message
                );

                showSuccess("Solicitação criada!");
                loadPending();

            } catch (Exception e) {
                showError(e.getMessage());
            }
        }
    }

    private void approveSelected() {
        List<Integer> selectedRows = getRowsToApprove();

        if (selectedRows.isEmpty()) {
            showWarning("Marque ou selecione ao menos uma solicitação.");
            return;
        }

        List<Integer> pendingIds = new ArrayList<>();
        int ignored = 0;

        for (int modelRow : selectedRows) {
            String status = tableModel.getValueAt(modelRow, 7).toString();

            if (!"PENDENTE".equalsIgnoreCase(status)) {
                ignored++;
                continue;
            }

            int id = getRequestIdAtRow(modelRow);

            if (id != -1) {
                pendingIds.add(id);
            }
        }

        if (pendingIds.isEmpty()) {
            showWarning("Nenhuma solicitação pendente foi selecionada. Apenas solicitações pendentes podem ser aprovadas.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Deseja aprovar " + pendingIds.size() + " solicitação(ões)?",
                "Confirmar aprovação em lote",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            int approved = 0;

            for (Integer id : pendingIds) {
                if (requestController.approve(id, loggedUser.getId())) {
                    approved++;
                }
            }

            if (approved == 0) {
                showWarning("Nenhuma solicitação foi aprovada.");
                return;
            }

            String message = approved + " solicitação(ões) aprovada(s) com sucesso.";

            if (ignored > 0) {
                message += "\n" + ignored + " solicitação(ões) ignorada(s) por não estarem pendentes.";
            }

            showSuccess(message);
            refreshCurrentView();
        }
    }

    private List<Integer> getRowsToApprove() {
        List<Integer> rows = new ArrayList<>();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object checked = tableModel.getValueAt(i, 1);

            if (Boolean.TRUE.equals(checked)) {
                rows.add(i);
            }
        }

        if (!rows.isEmpty()) {
            return rows;
        }

        int[] selectedRows = table.getSelectedRows();

        for (int selectedRow : selectedRows) {
            rows.add(table.convertRowIndexToModel(selectedRow));
        }

        return rows;
    }

    private void rejectSelected() {
        int modelRow = getSelectedModelRow();

        if (modelRow == -1) {
            showWarning("Selecione uma solicitação.");
            return;
        }

        String status = tableModel.getValueAt(modelRow, 7).toString();

        if (!"PENDENTE".equalsIgnoreCase(status)) {
            showWarning("Apenas solicitações pendentes podem ser rejeitadas.");
            return;
        }

        int id = getRequestIdAtRow(modelRow);

        if (id == -1) {
            showWarning("Solicitação inválida.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Deseja rejeitar esta solicitação?",
                "Confirmar rejeição",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            boolean rejected = requestController.reject(id);

            if (rejected) {
                showSuccess("Solicitação rejeitada!");
                refreshCurrentView();
            } else {
                showWarning("Não foi possível rejeitar esta solicitação.");
            }
        }
    }

    private void reopenSelected() {
        int modelRow = getSelectedModelRow();

        if (modelRow == -1) {
            showWarning("Selecione uma solicitação.");
            return;
        }

        String status = tableModel.getValueAt(modelRow, 7).toString();

        if ("APROVADO".equalsIgnoreCase(status)) {
            showWarning("Solicitações aprovadas já geraram lançamento financeiro e não podem ser reabertas automaticamente.");
            return;
        }

        if (!"REJEITADO".equalsIgnoreCase(status)) {
            showWarning("Apenas solicitações rejeitadas podem ser reabertas.");
            return;
        }

        int id = getRequestIdAtRow(modelRow);

        if (id == -1) {
            showWarning("Solicitação inválida.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Deseja reabrir esta solicitação?\n\nEla voltará para PENDENTE.",
                "Confirmar reabertura",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            boolean reopened = requestController.reopenRejected(id);

            if (reopened) {
                showSuccess("Solicitação reaberta com sucesso!");
                refreshCurrentView();
            } else {
                showWarning("Não foi possível reabrir esta solicitação.");
            }
        }
    }

    private void showRequestDetails() {
        int modelRow = getSelectedModelRow();

        if (modelRow == -1) {
            showWarning("Selecione uma solicitação.");
            return;
        }

        Object promoter = tableModel.getValueAt(modelRow, 2);
        Object companyLink = tableModel.getValueAt(modelRow, 3);
        Object type = tableModel.getValueAt(modelRow, 4);
        Object amount = tableModel.getValueAt(modelRow, 5);
        Object shortMessage = tableModel.getValueAt(modelRow, 6);
        Object status = tableModel.getValueAt(modelRow, 7);
        Object date = tableModel.getValueAt(modelRow, 8);
        Object fullMessage = tableModel.getValueAt(modelRow, 9);

        String messageToShow = fullMessage != null ? fullMessage.toString() : shortMessage.toString();
        String promoterPix = findPromoterPix(promoter.toString(), companyLink.toString());

        JDialog dialog = new JDialog(this, "Detalhes da Solicitação", true);
        dialog.setSize(760, 620);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(WHITE);

        JPanel header = new JPanel(null);
        header.setPreferredSize(new Dimension(760, 90));
        header.setBackground(BLACK);

        JLabel title = new JLabel("Solicitação - " + type);
        title.setForeground(WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setBounds(24, 16, 680, 30);
        header.add(title);

        JLabel subtitle = new JLabel("Promotor: " + promoter + " | Status: " + status);
        subtitle.setForeground(new Color(210, 210, 210));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setBounds(26, 50, 690, 22);
        header.add(subtitle);

        JPanel line = new JPanel();
        line.setBackground(ORANGE);
        line.setBounds(24, 80, 180, 4);
        header.add(line);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBackground(WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));

        JPanel info = new JPanel(new GridLayout(7, 1, 0, 7));
        info.setBackground(WHITE);

        info.add(createDetailRow("Promotor", promoter.toString()));
        info.add(createDetailRow("Vínculo", companyLink.toString()));
        info.add(createDetailRow("PIX", promoterPix));
        info.add(createDetailRow("Tipo", type.toString()));
        info.add(createDetailRow("Valor", amount.toString()));
        info.add(createDetailRow("Status", status.toString()));
        info.add(createDetailRow("Data", date.toString()));

        JTextArea messageArea = new JTextArea(messageToShow);
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageArea.setBackground(WHITE);
        messageArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane messageScroll = new JScrollPane(messageArea);
        messageScroll.setPreferredSize(new Dimension(700, 120));
        messageScroll.setBorder(BorderFactory.createTitledBorder("Mensagem / Dados para pagamento"));

        body.add(info, BorderLayout.CENTER);
        body.add(messageScroll, BorderLayout.SOUTH);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_GRAY));

        JButton close = createDarkButton("Fechar", 110);
        close.addActionListener(e -> dialog.dispose());
        footer.add(close);

        content.add(header, BorderLayout.NORTH);
        content.add(body, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private String findPromoterPix(String promoterName, String companyLink) {
        List<Promoter> promoters = promoterController.searchByNameIncludingInactive(promoterName);

        for (Promoter promoterData : promoters) {
            if (!promoterData.getName().equalsIgnoreCase(promoterName)) {
                continue;
            }

            if (promoterData.getCompanyLink() != null
                    && !promoterData.getCompanyLink().equalsIgnoreCase(companyLink)) {
                continue;
            }

            if (promoterData.getPix() != null && !promoterData.getPix().isBlank()) {
                return promoterData.getPix();
            }

            break;
        }

        return "Não informado";
    }

    private int getSelectedModelRow() {
        int row = table.getSelectedRow();

        if (row == -1) {
            return -1;
        }

        return table.convertRowIndexToModel(row);
    }

    private int getRequestIdAtRow(int modelRow) {
        Object idValue = tableModel.getValueAt(modelRow, 0);

        if (idValue instanceof Integer) {
            return (int) idValue;
        }

        try {
            return Integer.parseInt(idValue.toString());
        } catch (Exception e) {
            return -1;
        }
    }

    private JPanel createDetailRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY),
                BorderFactory.createEmptyBorder(7, 12, 7, 12)
        ));

        JLabel title = new JLabel(label);
        title.setPreferredSize(new Dimension(115, 24));
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setForeground(TEXT_GRAY);

        JLabel content = new JLabel(value == null || value.isBlank() ? "Não informado" : value);
        content.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        content.setForeground(BLACK);

        row.add(title, BorderLayout.WEST);
        row.add(content, BorderLayout.CENTER);

        return row;
    }

    private void addDetail(JPanel panel, String label, String value) {
        JPanel item = new JPanel(new BorderLayout(0, 3));
        item.setBackground(WHITE);
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

    private void exportPendingRequests() {
        List<String> pendingRequests = formatRequestLinesForExport(
                requestController.getPendingWithPromoterName()
        );

        if (pendingRequests.isEmpty()) {
            showWarning("Não existem solicitações pendentes para exportar.");
            return;
        }

        String path = util.FileSaveDialog.chooseXlsxPath(this, "solicitacoes_pendentes.xlsx");

        if (path == null) {
            return;
        }

        util.ExcelGenerator.generatePendingRequests(pendingRequests, path);

        showSuccess("Relatório de solicitações pendentes exportado com sucesso!");
    }

    private void exportPixBatch() {
        JSpinner paymentDateSpinner = new JSpinner(new SpinnerDateModel());
        paymentDateSpinner.setEditor(new JSpinner.DateEditor(paymentDateSpinner, "dd/MM/yyyy"));

        int dateOption = JOptionPane.showConfirmDialog(
                this,
                paymentDateSpinner,
                "Data do pagamento",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (dateOption != JOptionPane.OK_OPTION) {
            return;
        }

        java.util.Date selectedDate = (java.util.Date) paymentDateSpinner.getValue();

        LocalDate paymentDate = selectedDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        List<model.PromoterPaymentData> payments = requestController.getPendingPixBatch(paymentDate);

        if (payments.isEmpty()) {
            showWarning("Não há solicitações pendentes com PIX cadastrado.");
            return;
        }

        String path = util.FileSaveDialog.chooseXlsxPath(this, "pix_lote_solicitacoes.xlsx");

        if (path == null) {
            return;
        }

        util.ExcelGenerator.generatePixBatch(payments, path);

        showSuccess("Pix Lote exportado com sucesso!");
    }

    private void fillTable(List<String> lines) {
        tableModel.setRowCount(0);

        for (String line : lines) {
            addLineToTable(line);
        }

        updateSummary();
    }

    private void addLineToTable(String line) {
        try {
            String[] parts = line.split("\\|");

            int id = Integer.parseInt(parts[0].trim());
            String promoter = parts[1].replace("Promotor:", "").trim();
            String companyLink = parts[2]
                    .replace("Vínculo:", "")
                    .replace("Vinculo:", "")
                    .replace("VÃ­nculo:", "")
                    .trim();
            String type = requestController.getTypeLabel(parts[3].trim());
            String amount = formatMoney(parseMoney(parts[4].trim()));
            String fullMessage = parts[5].trim();
            String shortMessage = shortenText(fullMessage, 55);
            String status = parts[6].trim();
            String date = formatDateTime(parts[7].trim());

            tableModel.addRow(new Object[]{
                    id,
                    false,
                    promoter,
                    companyLink,
                    type,
                    amount,
                    shortMessage,
                    status,
                    date,
                    fullMessage
            });

        } catch (Exception e) {
            tableModel.addRow(new Object[]{
                    "-",
                    false,
                    "-",
                    "-",
                    "-",
                    "-",
                    shortenText(line, 55),
                    "-",
                    "-",
                    line
            });
        }
    }

    private void updateSummary() {
        int total = tableModel.getRowCount();
        int pending = 0;
        int approved = 0;
        int rejected = 0;

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String status = tableModel.getValueAt(i, 7).toString();

            if ("PENDENTE".equalsIgnoreCase(status)) {
                pending++;
            } else if ("APROVADO".equalsIgnoreCase(status)) {
                approved++;
            } else if ("REJEITADO".equalsIgnoreCase(status)) {
                rejected++;
            }
        }

        lblTotal.setText(String.valueOf(total));
        lblPending.setText(String.valueOf(pending));
        lblApproved.setText(String.valueOf(approved));
        lblRejected.setText(String.valueOf(rejected));
    }

    private List<String> formatRequestLinesForExport(List<String> lines) {
        List<String> formattedLines = new ArrayList<>();

        for (String line : lines) {
            try {
                String[] parts = line.split("\\|");

                if (parts.length >= 8) {
                    parts[3] = " " + requestController.getTypeLabel(parts[3].trim()) + " ";
                    parts[4] = " " + formatMoney(parseMoney(parts[4].trim())) + " ";
                    formattedLines.add(String.join("|", parts));
                } else {
                    formattedLines.add(line);
                }

            } catch (Exception e) {
                formattedLines.add(line);
            }
        }

        return formattedLines;
    }

    private JLabel createDialogLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(TEXT_GRAY);
        return label;
    }

    private JTextField createInputField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        return field;
    }

    private JButton createPrimaryButton(String text, int width) {
        JButton button = baseButton(text, width);
        button.setBackground(ORANGE);
        button.setForeground(WHITE);
        return button;
    }

    private JButton createSecondaryButton(String text, int width) {
        JButton button = baseButton(text, width);
        button.setBackground(WHITE);
        button.setForeground(BLACK);
        button.setBorder(BorderFactory.createLineBorder(BORDER_GRAY));
        return button;
    }

    private JButton createDarkButton(String text, int width) {
        JButton button = baseButton(text, width);
        button.setBackground(BLACK);
        button.setForeground(WHITE);
        return button;
    }

    private JButton createDangerButton(String text, int width) {
        JButton button = baseButton(text, width);
        button.setBackground(RED);
        button.setForeground(WHITE);
        return button;
    }

    private JButton baseButton(String text, int width) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(width, 38));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private boolean isRhUser() {
        return loggedUser.getJobTittle() != null
                && loggedUser.getJobTittle().equalsIgnoreCase("RH");
    }

    private boolean isFinanceUser() {
        return loggedUser.getJobTittle() != null
                && loggedUser.getJobTittle().equalsIgnoreCase("FINANCEIRO");
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

    private String convertTypeToDatabase(String type) {
        return requestController.toInternalType(type);
    }

    private String convertTypeToView(String type) {
        return requestController.getTypeLabel(type);
    }

    private String shortenText(String text, int maxLength) {
        if (text == null) {
            return "";
        }

        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength) + "...";
    }

    private String formatDateTime(String dateTime) {
        try {
            LocalDateTime dt = LocalDateTime.parse(dateTime);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return dt.format(formatter);
        } catch (Exception e) {
            return dateTime;
        }
    }

    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private BigDecimal parseMoney(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Informe um valor.");
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
            throw new IllegalArgumentException("O valor precisa ser maior que zero.");
        }

        return amount;
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return moneyFormatter.format(BigDecimal.ZERO).replace('\u00A0', ' ');
        }

        return moneyFormatter.format(value.setScale(2, RoundingMode.HALF_UP))
                .replace('\u00A0', ' ');
    }

    private class RequestTableRenderer extends DefaultTableCellRenderer {
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

            int modelRow = table.convertRowIndexToModel(row);
            String status = tableModel.getValueAt(modelRow, 7).toString();

            if (isSelected) {
                component.setBackground(new Color(255, 225, 205));
                component.setForeground(BLACK);
                return component;
            }

            if ("PENDENTE".equalsIgnoreCase(status)) {
                component.setBackground(YELLOW_BG);
            } else if ("APROVADO".equalsIgnoreCase(status)) {
                component.setBackground(GREEN_BG);
            } else if ("REJEITADO".equalsIgnoreCase(status)) {
                component.setBackground(RED_BG);
            } else {
                component.setBackground(WHITE);
            }

            component.setForeground(BLACK);
            return component;
        }
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
}