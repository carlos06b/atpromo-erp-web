# At Promo ERP

Sistema web para gestão operacional, financeira e de RH da empresa At Promo.

Esse projeto é a migração do antigo sistema desktop (Java Swing) para uma aplicação web moderna, com backend em Spring Boot (API REST) e frontend em React. O objetivo é manter todas as regras de negócio já existentes no sistema antigo, mas com uma interface mais acessível, responsiva e fácil de usar no dia a dia.

## Estrutura do repositório

```text
atpromo-erp-web/
├── backend/     # API REST em Spring Boot
└── frontend/    # Interface web em React
```

## Funcionalidades já migradas

- Login com autenticação por token (JWT)
- Cadastro de promotores, com CPF formatado automaticamente e tipo (CLT, MEI, Ferista)
- Cadastro de clientes/indústrias
- Faturamento, com visão em lista e em calendário (por vencimento, emissão e pagamento, coloridos por status)
- Folha de pagamento: lançamento de descontos e bônus por promotor (com busca pelo nome), fechamento do período com cálculo automático do líquido, e geração de lote Pix para promotores MEI
- Despesas fixas (com geração do próximo mês e histórico) e despesas variáveis (com parcelamento automático)
- Relatórios financeiros: resumo do período com resultado real e previsto, detalhamento de faturamento e de despesas, por período personalizável
- Solicitações de pagamento via Pix: RH cria a solicitação para um promotor, Financeiro aprova (o que já lança automaticamente no financeiro do promotor) ou rejeita, com exportação de lote Pix das solicitações pendentes
- Identificação do usuário logado (nome e cargo) na barra lateral
- Busca e filtros em todos os cadastros
- Exclusão protegida por senha em todos os cadastros, com aviso sobre o impacto em relatórios e históricos

## Funcionalidades pendentes

Nenhuma pendência conhecida no momento. Todos os módulos do sistema antigo já foram migrados, incluindo o fluxo de Solicitações de Pix.

## Tecnologias

**Backend**
- Java
- Spring Boot
- Spring Security (autenticação via JWT)
- Spring Data JPA / Hibernate
- MySQL
- Maven

**Frontend**
- React
- Vite
- Tailwind CSS
- React Router
- react-big-calendar

## Banco de dados

O backend usa MySQL. As credenciais ficam em `backend/src/main/resources/application-local.properties`, um arquivo que **não é versionado** (está no `.gitignore`) para não expor senhas no GitHub.

Para configurar:

1. Copie o arquivo de exemplo:

backend/src/main/resources/application-local.properties.example

para:

backend/src/main/resources/application-local.properties

2. Preencha com os dados do seu MySQL local e um segredo para o JWT:
```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/systematpromo
   spring.datasource.username=root
   spring.datasource.password=sua_senha

   jwt.secret=uma_chave_secreta_bem_grande_e_aleatoria
   jwt.expiration-ms=86400000
```

## Como executar

### Backend

1. Clone o repositório:

git clone https://github.com/carlos06b/atpromo-erp-web.git

2. Abra a pasta `backend` no IntelliJ IDEA.
3. Configure o `application-local.properties` (veja a seção acima).
4. Execute a classe principal do projeto Spring Boot.
5. A API sobe em `http://localhost:8080`.

### Frontend

1. Abra um terminal na pasta `frontend`.
2. Instale as dependências (só precisa na primeira vez ou quando alguma nova for adicionada):

npm install

3. Rode o servidor de desenvolvimento:

npm run dev

4. Acesse `http://localhost:5173` no navegador.

## Status

Projeto em desenvolvimento ativo — migração incremental do sistema desktop antigo para a nova arquitetura web.

## Autor

Carlos Laurindo
- GitHub: https://github.com/carlos06b