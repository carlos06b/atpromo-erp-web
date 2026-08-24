# SystemAtPromo

Sistema desktop desenvolvido em Java para auxiliar a gestão operacional, financeira e de RH da empresa At Promo.

O sistema centraliza cadastros, solicitações, despesas, faturamento, folha de pagamento, relatórios e exportações em Excel, reduzindo controles manuais e melhorando a organização interna.

## Funcionalidades

- Login de usuários
- Menus por área: RH e Financeiro
- Cadastro e controle de promotores
- Cadastro de clientes/indústrias
- Controle de solicitações financeiras
- Aprovação individual e em lote de solicitações
- Controle de faturamento
- Registro de valor faturado e valor recebido
- Controle de despesas fixas
- Geração mensal de despesas fixas
- Controle de despesas variáveis
- Cadastro de despesas variáveis parceladas
- Folha de pagamento
- Geração de PIX lote para MEIs
- Relatórios financeiros
- Exportação para Excel

## Tecnologias

- Java
- Java Swing
- MySQL
- JDBC
- Maven
- Apache POI
- Git/GitHub

## Banco de Dados

O sistema utiliza MySQL e acessa o banco por variáveis de ambiente.

Variáveis necessárias:

```text
DB_URL
DB_USER
DB_PASSWORD

Exemplo:

DB_URL=jdbc:mysql://localhost:3306/systematpromo
DB_USER=root
DB_PASSWORD=sua_senha

Como Executar

Clone o repositório:
git clone https://github.com/carlos06b/SystemAtPromo.git
Abra o projeto no IntelliJ IDEA.

Configure o banco de dados MySQL.

Configure as variáveis de ambiente.

Execute a classe principal do projeto.

```

## Status
Projeto em desenvolvimento ativo.

## Autor
Carlos Laurindo
- GitHub: https://github.com/carlos06b