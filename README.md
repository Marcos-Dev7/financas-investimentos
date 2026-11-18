<div align="center">
  <img src="src/main/resources/static/logo.png" alt="FinancialFamily Logo" width="96" style="border-radius: 22px;" />

  # FinancialFamily — Gestão Financeira & Patrimonial Familiar

  **Versão 1.0.0** &bull; Plataforma Full-Stack de Governança Financeira Familiar e Cálculo de Sobra Livre

  [![Version](https://img.shields.io/badge/version-1.0.0-10b981?style=for-the-badge)](https://github.com/Marcos-Dev7/financas-investimentos)
  [![Java](https://img.shields.io/badge/Java-25-f59e0b?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
  [![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.3-6db33f?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
  [![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Neon_Cloud-336791?style=for-the-badge&logo=postgresql&logoColor=white)](https://neon.tech/)
  [![Tests](https://img.shields.io/badge/Tests-76_Passing-10b981?style=for-the-badge&logo=junit5&logoColor=white)](#%EF%B8%8F-testes-automatizados)

  🌐 **Acesse a Plataforma Online:** [https://financial-family.onrender.com](https://financial-family.onrender.com)
</div>

---

## 📌 Sobre o Projeto

**FinancialFamily** é uma plataforma web completa (API REST + Interface Web Responsiva) desenvolvida para o controle financeiro familiar, focada na clareza de gastos, gestão de caixa e construção de patrimônio.

O sistema resolve um problema clássico do orçamento doméstico: separar o que é **conta prevista (Despesa Bruta)** do que já foi **efetivamente quitado (Despesa Paga)**, revelando em tempo real a verdadeira **Sobra Livre** mensal de cada usuário.

---

## ✨ Funcionalidades da Versão `1.0.0`

### 🔐 1. Autenticação, Segurança e Isolamento por Usuário
- **Cadastro com Política de Senha Forte:** Validação em tempo real (mínimo 8 caracteres, maiúscula, minúscula, número e caractere especial) + confirmação obrigatória de senha.
- **Verificação de E-mail com Código de 6 Dígitos:** Envio de código numérico temporário (`SecureRandom`, válido por 15 minutos) para ativação da conta.
- **Criptografia BCrypt & Sessão JWT:** Senhas armazenadas com hash adaptativo `BCryptPasswordEncoder` (Salt aleatório de 128 bits) e autenticação *stateless* via Token JWT (`Bearer`).
- **Isolamento Multi-Usuário:** Cada conta possui ambiente totalmente isolado — um usuário visualiza e gerencia exclusivamente suas próprias receitas, despesas e cálculos.

### 👤 2. Área do Usuário ("Minha Conta")
- **Upload de Foto de Perfil:** Recorte quadrado e compressão automática (`256x256`) no navegador, com persistência direta no banco de dados na nuvem.
- **Gestão de Dados Cadastrais:** Atualização de nome completo e endereço de e-mail com renovação automática do token de sessão.
- **Segurança & Privacidade:** Alteração de senha (mediante confirmação da senha atual) e opção de exclusão permanente da conta e seus registros.

### 💰 3. Gestão de Despesas, Receitas e Motor de Sobra Livre
- **Despesas Mensais:** Cadastro, edição, exclusão, paginação, busca por descrição/categoria, baixa de pagamento com 1 clique (`Pagar`) e reversão (`Estornar`).
- **Saldos e Receitas:** Registro de múltiplas fontes de renda por mês/ano (Salários, Freelances, Bônus).
- **Motor de Cálculo de Sobra Livre:** Calcula automaticamente `Receita Total - Despesas Pagas`, classificando a saúde do mês em `POSITIVO`, `LIMITE` ou `DEFICIT`.
- **Painel Analítico:** Gráfico de distribuição percentual de gastos por categoria e termômetro de comprometimento da renda mensal.
- **Agendador Automático (`@Scheduled`):** Projeta automaticamente todas as despesas fixas para o mês seguinte na virada de cada dia 1º.

---

## 📖 Como Utilizar o Sistema (Guia do Usuário)

1. **Criar sua Conta:**
   - Acesse a plataforma e clique na aba **"Criar Conta"**.
   - Informe seu nome, e-mail e crie uma senha que atenda aos 5 critérios do indicador visual (com confirmação de senha).
   - Digite o **código de 6 dígitos** enviado para o seu e-mail para ativar sua conta e entrar automaticamente.
2. **Cadastrar suas Fontes de Renda (Saldos):**
   - No painel principal, selecione o **Mês de Referência** no topo da tela.
   - No card lateral **"Saldos & Receitas"**, clique em **`+ Nova Receita`** e adicione suas entradas do mês (ex.: *Salário Mensal*).
3. **Registrar e Dar Baixa nas Despesas:**
   - Clique em **`+ Nova Despesa`**, preencha descrição, valor, data de vencimento, categoria (ex.: *Moradia*, *Alimentação*) e marque **"Despesa Fixa Mensal"** caso ela se repita todo mês.
   - Conforme for pagando suas contas no mês, clique no botão **`Pagar`** na linha da despesa (ou **`Estornar`** caso queira desfazer).
4. **Acompanhar a Sobra Livre e Análises:**
   - Os 4 indicadores no topo mostrarão em tempo real a **Receita Total**, **Despesas Pagas**, **Despesas Brutas (Previsto)** e a sua **Sobra Livre Disponível**, além da divisão por categoria logo abaixo.
5. **Personalizar seu Perfil ("Minha Conta"):**
   - Clique em **"Minha Conta"** no menu superior (ou sobre sua foto/nome) para enviar sua **foto de perfil**, atualizar seu nome/e-mail ou alterar sua senha.

---

## 🛠️ Stack Tecnológica

| Camada | Tecnologias |
| :--- | :--- |
| **Linguagem & Runtime** | Java 25 |
| **Framework Backend** | Spring Boot 4.0.3 (WebMVC, Validation, Scheduling, Mail) |
| **Segurança** | Spring Security 6 + Auth0 Java JWT (`HMAC256`) + BCrypt |
| **Banco de Dados & Migrations** | PostgreSQL (Neon Serverless) + Spring Data JPA / Hibernate + Flyway (`V1` a `V8`) |
| **Frontend** | HTML5 Semântico, CSS3 Custom Design System (*Obsidian & Emerald-Gold*), Vanilla JS ES6+ & Canvas API |
| **Testes** | JUnit 5, Mockito, AssertJ e Spring `MockMvc` (76 testes automatizados) |
| **Infraestrutura & Deploy** | Docker (Multi-Stage Build), Docker Compose, Render & Neon Cloud |

---

## 🔌 Principais Endpoints da API REST

### Autenticação & Conta (`/auth`)
| Método | Rota | Descrição | Autenticação |
| :--- | :--- | :--- | :---: |
| `POST` | `/auth/registrar` | Cadastra usuário e envia código de 6 dígitos | Pública |
| `POST` | `/auth/verificar-email` | Valida o código de 6 dígitos e retorna Token JWT | Pública |
| `POST` | `/auth/reenviar-codigo` | Reenvia novo código de verificação | Pública |
| `POST` | `/auth/login` | Autentica e-mail/senha e retorna Token JWT | Pública |
| `GET` | `/auth/me` | Retorna dados e foto do usuário autenticado | `Bearer JWT` |
| `PUT` | `/auth/perfil` | Atualiza nome, e-mail e foto de perfil | `Bearer JWT` |
| `PUT` | `/auth/alterar-senha` | Altera a senha validando a senha atual e força | `Bearer JWT` |
| `DELETE` | `/auth/conta` | Exclui permanentemente a conta e seus dados | `Bearer JWT` |

### Finanças (`/despesas`, `/saldos`, `/calculos`)
| Método | Rota | Descrição | Autenticação |
| :--- | :--- | :--- | :---: |
| `GET` | `/despesas` | Lista despesas do usuário (paginado) | `Bearer JWT` |
| `POST` | `/despesas` | Cadastra nova despesa | `Bearer JWT` |
| `PUT` | `/despesas/{id}` | Atualiza dados de uma despesa | `Bearer JWT` |
| `PATCH` | `/despesas/{id}/pagar` | Dá baixa na despesa (`isPaga = true`) | `Bearer JWT` |
| `PATCH` | `/despesas/{id}/estornar` | Estorna o pagamento (`isPaga = false`) | `Bearer JWT` |
| `DELETE` | `/despesas/{id}` | Remove uma despesa | `Bearer JWT` |
| `GET` | `/saldos` | Lista todas as receitas do usuário | `Bearer JWT` |
| `POST` | `/saldos` | Cadastra nova fonte de receita | `Bearer JWT` |
| `PUT` | `/saldos/{id}` | Atualiza uma receita existente | `Bearer JWT` |
| `DELETE` | `/saldos/{id}` | Remove uma receita | `Bearer JWT` |
| `GET` | `/calculos/sobra/{mes}` | Retorna o resumo mensal e a Sobra Livre | `Bearer JWT` |

---

## 💻 Como Rodar o Projeto Localmente

### Opção 1: Com Docker Compose (Mais rápida — sobe API + PostgreSQL)
```bash
git clone https://github.com/Marcos-Dev7/financas-investimentos.git
cd financas-investimentos
docker compose up --build
```
Acesse em seguida: **`http://localhost:8080`**

### Opção 2: Com Maven + Arquivo `.env`
1. Crie um arquivo `.env` na raiz do projeto com as variáveis:
   ```env
   DB_URL=jdbc:postgresql://localhost:5432/financas_db
   DB_USERNAME=postgres
   DB_PASSWORD=sua_senha
   JWT_SECRET=sua_chave_secreta_jwt_aqui

   # Opcional (para envio externo de e-mail via Brevo HTTPS API ou SMTP)
   BREVO_API_KEY=
   MAIL_FROM=
   ```
2. Execute a aplicação:
   ```bash
   ./mvnw spring-boot:run
   ```
3. Abra **`http://localhost:8080`** no navegador.

---

## 🧪 Testes Automatizados

O projeto conta com **76 testes automatizados** cobrindo Entidades de Domínio, Enums, Conversores, Validador de Senha Forte, Services (Mockito) e Controllers (MockMvc). Para executá-los:

```bash
./mvnw test -Dtest="*Test"
```

---

## 👤 Autor

Desenvolvido por **Marcos Paulo de Freitas Pereira** — Estudante de Engenharia de Software e Desenvolvedor Backend Java.

- **LinkedIn:** [linkedin.com/in/marcos-paulo7](https://www.linkedin.com/in/marcos-paulo7)
- **Instagram:** [@__.kiin](https://www.instagram.com/__.kiin)
- **GitHub:** [github.com/Marcos-Dev7](https://github.com/Marcos-Dev7)
