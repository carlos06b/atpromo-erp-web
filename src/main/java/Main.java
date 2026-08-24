import controller.*;
import model.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        UserController userController = new UserController();
        RequestController requestController = new RequestController();
        FinanceController financeController = new FinanceController();
        PromoterController promoterController = new PromoterController();
        FixedExpenseController fixedExpenseController = new FixedExpenseController();
        FixedExpenseHistoryController fixedExpenseHistoryController = new FixedExpenseHistoryController();
        VariableExpenseController variableExpenseController = new VariableExpenseController();
        ReportController reportController = new ReportController();

        printTitle("SISTEMA AT PROMO");

        System.out.print("Email: ");
        String email = sc.nextLine().trim();

        System.out.print("Senha: ");
        String password = sc.nextLine().trim();

        User loggedUser = userController.login(email, password);

        if (loggedUser == null) {
            printError("Email ou senha inválidos. Encerrando sistema...");
            return;
        }

        printSuccess("Login realizado com sucesso!");
        System.out.println("Usuário: " + loggedUser.getName());
        System.out.println("Cargo: " + loggedUser.getJobTittle());

        if (loggedUser.getJobTittle().trim().equalsIgnoreCase("RH")) {
            menuRH(sc, loggedUser, requestController, promoterController);
        } else if (loggedUser.getJobTittle().trim().equalsIgnoreCase("FINANCEIRO")) {
            menuFinanceiro(
                    sc,
                    requestController,
                    financeController,
                    fixedExpenseController,
                    fixedExpenseHistoryController,
                    variableExpenseController,
                    reportController
            );
        } else {
            printError("Cargo sem permissão.");
        }

        sc.close();
    }

    public static void menuRH(Scanner sc, User loggedUser, RequestController requestController, PromoterController promoterController) {
        int option;

        do {
            printTitle("MENU RH");
            System.out.println("1 - Solicitações");
            System.out.println("2 - Promotores");
            System.out.println("0 - Sair");

            option = readInt(sc, "Escolha: ");

            switch (option) {
                case 1 -> menuRHRequests(sc, loggedUser, requestController);
                case 2 -> menuPromoters(sc, promoterController);
                case 0 -> printInfo("Saindo do menu RH...");
                default -> printError("Opção inválida.");
            }

        } while (option != 0);
    }

    public static void menuRHRequests(Scanner sc, User loggedUser, RequestController requestController) {
        int option;

        do {
            printTitle("SOLICITAÇÕES RH");
            System.out.println("1 - Criar solicitação");
            System.out.println("2 - Listar solicitações");
            System.out.println("3 - Listar por período");
            System.out.println("0 - Voltar");

            option = readInt(sc, "Escolha: ");

            switch (option) {
                case 1 -> createRequest(sc, loggedUser, requestController);
                case 2 -> requestController.listAllWithPromoterName();
                case 3 -> listRequestsByPeriod(sc, requestController);
                case 0 -> printInfo("Voltando...");
                default -> printError("Opção inválida.");
            }

        } while (option != 0);
    }

    public static void menuPromoters(Scanner sc, PromoterController promoterController) {
        int option;

        do {
            printTitle("PROMOTORES");
            System.out.println("1 - Cadastrar promotor");
            System.out.println("2 - Listar promotores");
            System.out.println("3 - Buscar promotor");
            System.out.println("4 - Atualizar promotor");
            System.out.println("5 - Inativar promotor");
            System.out.println("0 - Voltar");

            option = readInt(sc, "Escolha: ");

            switch (option) {
                case 1 -> registerPromoter(sc, promoterController);
                case 2 -> promoterController.listAll();
                case 3 -> promoterController.findById(readInt(sc, "ID do promotor: "));
                case 4 -> updatePromoter(sc, promoterController);
                case 5 -> promoterController.inactivate(readInt(sc, "ID do promotor: "));
                case 0 -> printInfo("Voltando...");
                default -> printError("Opção inválida.");
            }

        } while (option != 0);
    }

    public static void menuFinanceiro(
            Scanner sc,
            RequestController requestController,
            FinanceController financeController,
            FixedExpenseController fixedExpenseController,
            FixedExpenseHistoryController fixedExpenseHistoryController,
            VariableExpenseController variableExpenseController,
            ReportController reportController
    ) {
        int option;

        do {
            printTitle("MENU FINANCEIRO");
            System.out.println("1 - Solicitações");
            System.out.println("2 - Relatórios financeiros");
            System.out.println("3 - Despesas fixas");
            System.out.println("4 - Histórico mensal de despesas fixas");
            System.out.println("5 - Despesas variáveis");
            System.out.println("0 - Sair");

            option = readInt(sc, "Escolha: ");

            switch (option) {
                case 1 -> menuFinanceRequests(sc, requestController);
                case 2 -> menuReports(sc, financeController, reportController);
                case 3 -> menuFixedExpenses(sc, fixedExpenseController);
                case 4 -> menuFixedExpenseHistory(sc, fixedExpenseHistoryController);
                case 5 -> menuVariableExpenses(sc, variableExpenseController);
                case 0 -> printInfo("Saindo do menu financeiro...");
                default -> printError("Opção inválida.");
            }

        } while (option != 0);
    }

    public static void menuFinanceRequests(Scanner sc, RequestController requestController) {
        int option;

        do {
            printTitle("SOLICITAÇÕES FINANCEIRO");
            System.out.println("1 - Listar pendentes");
            System.out.println("2 - Listar todas");
            System.out.println("3 - Aprovar");
            System.out.println("4 - Rejeitar");
            System.out.println("5 - Listar por período");
            System.out.println("0 - Voltar");

            option = readInt(sc, "Escolha: ");

            switch (option) {
                case 1 -> requestController.listPendingWithPromoterName();
                case 2 -> requestController.listAllWithPromoterName();
                case 3 -> requestController.approve(readInt(sc, "ID da solicitação: "));
                case 4 -> requestController.reject(readInt(sc, "ID da solicitação: "));
                case 5 -> listRequestsByPeriod(sc, requestController);
                case 0 -> printInfo("Voltando...");
                default -> printError("Opção inválida.");
            }

        } while (option != 0);
    }

    public static void menuReports(Scanner sc, FinanceController financeController, ReportController reportController) {
        int option;

        do {
            printTitle("RELATÓRIOS FINANCEIROS");
            System.out.println("1 - Listar financeiro por período");
            System.out.println("2 - Relatório financeiro completo");
            System.out.println("3 - Relatório financeiro por tipo");
            System.out.println("4 - Relatório geral da empresa");
            System.out.println("0 - Voltar");

            option = readInt(sc, "Escolha: ");

            switch (option) {
                case 1 -> listFinanceByPeriod(sc, financeController);
                case 2 -> reportFinanceByPeriod(sc, financeController);
                case 3 -> reportFinanceByTypeAndPeriod(sc, financeController);
                case 4 -> reportGeneralCompany(sc, reportController);
                case 0 -> printInfo("Voltando...");
                default -> printError("Opção inválida.");
            }

        } while (option != 0);
    }

    public static void menuFixedExpenses(Scanner sc, FixedExpenseController fixedExpenseController) {
        int option;

        do {
            printTitle("DESPESAS FIXAS");
            System.out.println("1 - Cadastrar");
            System.out.println("2 - Listar todas");
            System.out.println("3 - Listar pendentes");
            System.out.println("4 - Listar pagas");
            System.out.println("5 - Marcar como paga");
            System.out.println("6 - Excluir");
            System.out.println("0 - Voltar");

            option = readInt(sc, "Escolha: ");

            switch (option) {
                case 1 -> registerFixedExpense(sc, fixedExpenseController);
                case 2 -> fixedExpenseController.listAll();
                case 3 -> fixedExpenseController.listByStatus(false);
                case 4 -> fixedExpenseController.listByStatus(true);
                case 5 -> markFixedExpenseAsPaid(sc, fixedExpenseController);
                case 6 -> fixedExpenseController.delete(readInt(sc, "ID da despesa fixa: "));
                case 0 -> printInfo("Voltando...");
                default -> printError("Opção inválida.");
            }

        } while (option != 0);
    }

    public static void menuFixedExpenseHistory(Scanner sc, FixedExpenseHistoryController controller) {
        int option;

        do {
            printTitle("HISTÓRICO MENSAL DE DESPESAS FIXAS");
            System.out.println("1 - Gerar despesas do mês");
            System.out.println("2 - Listar por período");
            System.out.println("3 - Listar pendentes");
            System.out.println("4 - Listar pagas");
            System.out.println("5 - Marcar como paga");
            System.out.println("6 - Total por período");
            System.out.println("0 - Voltar");

            option = readInt(sc, "Escolha: ");

            switch (option) {
                case 1 -> {
                    int month = readMonth(sc);
                    int year = readYear(sc);
                    controller.generateMonthlyExpenses(month, year);
                }
                case 2 -> listFixedExpenseHistoryByPeriod(sc, controller);
                case 3 -> listFixedExpenseHistoryByStatus(sc, controller, "PENDENTE");
                case 4 -> listFixedExpenseHistoryByStatus(sc, controller, "PAGO");
                case 5 -> markFixedExpenseHistoryAsPaid(sc, controller);
                case 6 -> totalFixedExpenseHistoryByPeriod(sc, controller);
                case 0 -> printInfo("Voltando...");
                default -> printError("Opção inválida.");
            }

        } while (option != 0);
    }

    public static void menuVariableExpenses(Scanner sc, VariableExpenseController controller) {
        int option;

        do {
            printTitle("DESPESAS VARIÁVEIS");
            System.out.println("1 - Cadastrar");
            System.out.println("2 - Listar por período");
            System.out.println("3 - Marcar como paga");
            System.out.println("4 - Excluir");
            System.out.println("0 - Voltar");

            option = readInt(sc, "Escolha: ");

            switch (option) {
                case 1 -> registerVariableExpense(sc, controller);
                case 2 -> listVariableExpenseByPeriod(sc, controller);
                case 3 -> markVariableExpenseAsPaid(sc, controller);
                case 4 -> controller.delete(readInt(sc, "ID da despesa variável: "));
                case 0 -> printInfo("Voltando...");
                default -> printError("Opção inválida.");
            }

        } while (option != 0);
    }

    public static void createRequest(Scanner sc, User loggedUser, RequestController controller) {
        int idFinanceiro = readInt(sc, "ID do usuário financeiro: ");
        int idPromoter = readInt(sc, "ID do promotor: ");
        String type = readType(sc, "Tipo da solicitação: ");
        BigDecimal amount = readPositiveBigDecimal(sc, "Valor: ");

        System.out.print("Mensagem: ");
        String message = sc.nextLine().trim();

        if (message.isBlank()) {
            printError("Mensagem não pode ficar vazia.");
            return;
        }

        controller.createRequest(
                loggedUser.getId(),
                idFinanceiro,
                idPromoter,
                type,
                amount,
                message
        );
    }

    public static void registerPromoter(Scanner sc, PromoterController controller) {
        System.out.print("Nome: ");
        String name = sc.nextLine().trim();

        System.out.print("CPF: ");
        String cpf = sc.nextLine().trim();

        System.out.print("Telefone: ");
        String phone = sc.nextLine().trim();

        String uf = readNortheastUf(sc);

        System.out.print("Cidade: ");
        String city = sc.nextLine().trim();

        String companyLink = readCompanyLink(sc);

        System.out.print("PIX: ");
        String pix = sc.nextLine().trim();

        String pixType = readPixType(sc);

        if (name.isBlank() || cpf.isBlank() || phone.isBlank() || city.isBlank()) {
            printError("Nome, CPF, telefone, UF e cidade são obrigatórios.");
            return;
        }

        LocalDate dateBirth = readDate(sc, "Data de nascimento (AAAA-MM-DD): ");
        BigDecimal salary = readPositiveBigDecimal(sc, "Salário: ");
        String type = readPromoterType(sc);

        controller.register(name, cpf, phone, uf, city, companyLink, pix, pixType, dateBirth, salary, type);
    }

    public static void updatePromoter(Scanner sc, PromoterController controller) {
        int id = readInt(sc, "ID do promotor: ");

        System.out.print("Novo nome: ");
        String name = sc.nextLine().trim();

        System.out.print("Novo telefone: ");
        String phone = sc.nextLine().trim();

        String uf = readNortheastUf(sc);

        System.out.print("Nova cidade: ");
        String city = sc.nextLine().trim();

        String companyLink = readCompanyLink(sc);

        System.out.print("Novo PIX: ");
        String pix = sc.nextLine().trim();

        String pixType = readPixType(sc);

        if (name.isBlank() || phone.isBlank() || city.isBlank()) {
            printError("Nome, telefone, UF e cidade são obrigatórios.");
            return;
        }

        BigDecimal salary = readPositiveBigDecimal(sc, "Novo salário: ");
        String type = readPromoterType(sc);

        controller.update(id, name, phone, uf, city, companyLink, pix, pixType, salary, type);
    }

    public static void registerFixedExpense(Scanner sc, FixedExpenseController controller) {
        System.out.print("Nome da despesa: ");
        String name = sc.nextLine().trim();

        if (name.isBlank()) {
            printError("Nome da despesa não pode ficar vazio.");
            return;
        }

        BigDecimal amount = readPositiveBigDecimal(sc, "Valor: ");
        LocalDate dueDate = readDate(sc, "Data de vencimento (AAAA-MM-DD): ");

        controller.register(name, amount, dueDate);
    }

    public static void markFixedExpenseAsPaid(Scanner sc, FixedExpenseController controller) {
        int id = readInt(sc, "ID da despesa fixa: ");
        LocalDate paymentDate = readDate(sc, "Data de pagamento (AAAA-MM-DD): ");

        controller.markAsPaid(id, paymentDate);
    }

    public static void registerVariableExpense(Scanner sc, VariableExpenseController controller) {
        System.out.print("Nome da despesa: ");
        String name = sc.nextLine().trim();

        if (name.isBlank()) {
            printError("Nome da despesa não pode ficar vazio.");
            return;
        }

        BigDecimal amount = readPositiveBigDecimal(sc, "Valor: ");
        LocalDate date = readDate(sc, "Data da despesa (AAAA-MM-DD): ");

        System.out.print("Descrição: ");
        String description = sc.nextLine().trim();

        if (description.isBlank()) {
            description = "Sem descrição";
        }

        controller.registerVariableExpense(name, amount, date, description);
    }

    public static void listVariableExpenseByPeriod(Scanner sc, VariableExpenseController controller) {
        LocalDate start = readDate(sc, "Data início (AAAA-MM-DD): ");
        LocalDate end = readDate(sc, "Data fim (AAAA-MM-DD): ");

        if (!isValidPeriod(start, end)) return;

        controller.listByPeriod(start, end);
    }

    public static void markVariableExpenseAsPaid(Scanner sc, VariableExpenseController controller) {
        int id = readInt(sc, "ID da despesa variável: ");
        controller.markAsPaid(id);
    }

    public static void listRequestsByPeriod(Scanner sc, RequestController controller) {
        LocalDate start = readDate(sc, "Data início (AAAA-MM-DD): ");
        LocalDate end = readDate(sc, "Data fim (AAAA-MM-DD): ");

        if (!isValidPeriod(start, end)) return;

        controller.listByPeriod(start.atStartOfDay(), end.atTime(23, 59, 59));
    }

    public static void listFinanceByPeriod(Scanner sc, FinanceController controller) {
        LocalDate start = readDate(sc, "Data início (AAAA-MM-DD): ");
        LocalDate end = readDate(sc, "Data fim (AAAA-MM-DD): ");

        if (!isValidPeriod(start, end)) return;

        controller.listByPeriod(start, end);
    }

    public static void reportFinanceByPeriod(Scanner sc, FinanceController controller) {
        LocalDate start = readDate(sc, "Data início (AAAA-MM-DD): ");
        LocalDate end = readDate(sc, "Data fim (AAAA-MM-DD): ");

        if (!isValidPeriod(start, end)) return;

        controller.showReportByPeriod(start, end);
    }

    public static void reportFinanceByTypeAndPeriod(Scanner sc, FinanceController controller) {
        LocalDate start = readDate(sc, "Data início (AAAA-MM-DD): ");
        LocalDate end = readDate(sc, "Data fim (AAAA-MM-DD): ");

        if (!isValidPeriod(start, end)) return;

        controller.showReportByTypeAndPeriod(start, end);
    }

    public static void reportGeneralCompany(Scanner sc, ReportController controller) {
        LocalDate start = readDate(sc, "Data início (AAAA-MM-DD): ");
        LocalDate end = readDate(sc, "Data fim (AAAA-MM-DD): ");

        if (!isValidPeriod(start, end)) return;

        controller.showGeneralReport(start, end);
    }

    public static void listFixedExpenseHistoryByPeriod(Scanner sc, FixedExpenseHistoryController controller) {
        LocalDate start = readDate(sc, "Data início (AAAA-MM-DD): ");
        LocalDate end = readDate(sc, "Data fim (AAAA-MM-DD): ");

        if (!isValidPeriod(start, end)) return;

        controller.listByPeriod(start, end);
    }

    public static void listFixedExpenseHistoryByStatus(
            Scanner sc,
            FixedExpenseHistoryController controller,
            String status
    ) {
        LocalDate start = readDate(sc, "Data início (AAAA-MM-DD): ");
        LocalDate end = readDate(sc, "Data fim (AAAA-MM-DD): ");

        if (!isValidPeriod(start, end)) return;

        controller.listByStatus(start, end, status);
    }

    public static void markFixedExpenseHistoryAsPaid(Scanner sc, FixedExpenseHistoryController controller) {
        int id = readInt(sc, "ID da despesa fixa mensal: ");
        LocalDate paymentDate = readDate(sc, "Data de pagamento (AAAA-MM-DD): ");

        controller.markAsPaid(id, paymentDate);
    }

    public static void totalFixedExpenseHistoryByPeriod(Scanner sc, FixedExpenseHistoryController controller) {
        LocalDate start = readDate(sc, "Data início (AAAA-MM-DD): ");
        LocalDate end = readDate(sc, "Data fim (AAAA-MM-DD): ");

        if (!isValidPeriod(start, end)) return;

        controller.showTotalByPeriod(start, end);
    }

    public static int readInt(Scanner sc, String message) {
        while (true) {
            try {
                System.out.print(message);
                return Integer.parseInt(sc.nextLine().trim());
            } catch (Exception e) {
                printError("Número inválido. Digite um número inteiro.");
            }
        }
    }

    public static int readMonth(Scanner sc) {
        while (true) {
            int month = readInt(sc, "Mês (1-12): ");

            if (month >= 1 && month <= 12) return month;

            printError("Mês inválido. Digite um número entre 1 e 12.");
        }
    }

    public static int readYear(Scanner sc) {
        while (true) {
            int year = readInt(sc, "Ano: ");

            if (year >= 2000 && year <= 2100) return year;

            printError("Ano inválido.");
        }
    }

    public static BigDecimal readBigDecimal(Scanner sc, String message) {
        while (true) {
            try {
                System.out.print(message);
                String input = sc.nextLine().trim().replace(",", ".");
                return new BigDecimal(input);
            } catch (Exception e) {
                printError("Valor inválido. Exemplo: 500.00");
            }
        }
    }

    public static BigDecimal readPositiveBigDecimal(Scanner sc, String message) {
        while (true) {
            BigDecimal value = readBigDecimal(sc, message);

            if (value.compareTo(BigDecimal.ZERO) > 0) {
                return value;
            }

            printError("O valor precisa ser maior que zero.");
        }
    }

    public static LocalDate readDate(Scanner sc, String message) {
        while (true) {
            try {
                System.out.print(message);
                return LocalDate.parse(sc.nextLine().trim());
            } catch (Exception e) {
                printError("Data inválida. Use o formato AAAA-MM-DD.");
            }
        }
    }

    public static String readPromoterType(Scanner sc) {
        while (true) {
            System.out.print("Tipo (CLT/MEI/FERISTA): ");
            String type = sc.nextLine().trim().toUpperCase();

            if (type.equals("CLT") || type.equals("MEI") || type.equals("FERISTA")) {
                return type;
            }

            printError("Tipo inválido. Use CLT, MEI ou FERISTA.");
        }
    }

    public static String readCompanyLink(Scanner sc) {
        while (true) {
            System.out.print("Vínculo (AT/TEJO): ");
            String companyLink = sc.nextLine().trim().toUpperCase();

            if (companyLink.equals("AT") || companyLink.equals("TEJO")) {
                return companyLink;
            }

            printError("Vínculo inválido. Use AT ou TEJO.");
        }
    }

    public static String readNortheastUf(Scanner sc) {
        while (true) {
            System.out.print("UF (MA/PI/CE/RN/PB/PE/AL/SE/BA): ");
            String uf = sc.nextLine().trim().toUpperCase();

            if (uf.equals("MA")
                    || uf.equals("PI")
                    || uf.equals("CE")
                    || uf.equals("RN")
                    || uf.equals("PB")
                    || uf.equals("PE")
                    || uf.equals("AL")
                    || uf.equals("SE")
                    || uf.equals("BA")) {
                return uf;
            }

            printError("UF inválida. Use uma UF do Nordeste.");
        }
    }

    public static String readType(Scanner sc, String message) {
        while (true) {
            System.out.print(message);
            String type = sc.nextLine().trim().toUpperCase();

            if (!type.isBlank()) {
                return type;
            }

            printError("Tipo não pode ficar vazio.");
        }
    }

    public static boolean isValidPeriod(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            printError("Data inicial não pode ser maior que a final.");
            return false;
        }

        return true;
    }

    public static String readPixType(Scanner sc) {
        while (true) {
            System.out.print("Tipo do PIX (TELEFONE/EMAIL/CPF/CNPJ/ALEATORIA): ");
            String pixType = sc.nextLine().trim().toUpperCase();

            if (pixType.equals("TELEFONE")
                    || pixType.equals("EMAIL")
                    || pixType.equals("CPF")
                    || pixType.equals("CNPJ")
                    || pixType.equals("ALEATORIA")) {
                return pixType;
            }

            printError("Tipo do PIX inválido. Use TELEFONE, EMAIL, CPF, CNPJ ou ALEATORIA.");
        }
    }

    public static void printTitle(String title) {
        System.out.println("\n====================================");
        System.out.println(" " + title);
        System.out.println("====================================");
    }

    public static void printSuccess(String message) {
        System.out.println("OK - " + message);
    }

    public static void printError(String message) {
        System.out.println("ERRO - " + message);
    }

    public static void printInfo(String message) {
        System.out.println("- " + message);
    }
}