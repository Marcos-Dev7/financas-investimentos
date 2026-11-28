<div align="center">
  <img src="src/main/resources/static/logo.png" alt="FinancialFamily Logo" width="96" style="border-radius: 22px;" />

  # FinancialFamily — Gestão Financeira & Patrimonial Familiar

  **Versão 2.0.0** &bull; Plataforma Full-Stack de Governança Financeira Familiar, Membros & Cálculo de Sobra Livre

  [![Version](https://img.shields.io/badge/version-2.0.0-2563eb?style=for-the-badge)](https://github.com/Marcos-Dev7/financas-investimentos)
  [![Java](https://img.shields.io/badge/Java-25-f59e0b?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
  [![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.3-6db33f?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
  [![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-336791?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
  [![Tests](https://img.shields.io/badge/Tests-123_Passing-10b981?style=for-the-badge&logo=junit5&logoColor=white)](#%EF%B8%8F-testes-automatizados)

  🌐 **Acesse a Plataforma Online:** [https://financial-family.onrender.com](https://financial-family.onrender.com)
</div>

---

## 📌 Sobre o Projeto

**FinancialFamily** é uma plataforma web completa (API REST + Interface Web Responsiva Desktop & Mobile) desenvolvida para o controle financeiro pessoal e familiar, focada na clareza de gastos, gestão compartilhada de caixa e acompanhamento do orçamento mensal.

O sistema separa o que é **conta prevista (Despesa Bruta)** do que já foi **efetivamente quitado (Despesa Paga)**, revelando em tempo real a **Sobra Livre** mensal — tanto no perfil individual quanto na **Visão Consolidada da Família**.

---

## ✨ Funcionalidades Principais (`v2.0.0`)

### 👨‍👩‍👧‍👦 1. Módulo Família & Membros
- **Convites com Prazo de Validade:** Convide familiares por e-mail ou código de convite diretamente em **Minha Conta**.
- **Visão Consolidada & Papéis de Acesso:** Painel dedicado que reúne as despesas compartilhadas da família com filtro por responsável, total por membro e distribuição por categoria. O criador do grupo atua como **Gestor** (podendo dar baixa e estornar pagamentos nas despesas compartilhadas), enquanto membros convidados visualizam o consolidado em modo **Somente Leitura**.
- **Compartilhamento Granular (*Opt-in / Opt-out*):** Cada participante escolhe individualmente se deseja compartilhar sua renda e/ou suas despesas no consolidado familiar, podendo alterar ou revogar o vínculo a qualquer momento.

### 🔐 2. Conta, Segurança & Privacidade (LGPD)
- **Autenticação Segura & Verificação em 2 Fatores (2FA):** Suporte a confirmação em duas etapas via e-mail ou aplicativo autenticador (Google Authenticator, Authy, Apple Senhas), além de confirmação de e-mail no cadastro e na alteração de endereço.
- **Recuperação de Acesso:** Fluxo completo de recuperação de senha via e-mail principal, e-mail secundário ou telefone cadastrado.
- **Proteção de Conta & Dados:** Política de senha forte, isolamento estrito de dados por usuário, proteção contra tentativas abusivas de acesso, cabeçalhos de segurança web e conformidade com a LGPD (banner de cookies configurável, Termos de Uso e Política de Privacidade integrados à interface).

### 💰 3. Gestão de Despesas, Receitas & Sobra Livre
- **Despesas Mensais:** Cadastro, edição, exclusão, busca por descrição ou categoria, controle de contas fixas/variáveis, baixa de pagamento com 1 clique e estorno.
- **Receitas & Fontes de Renda:** Registro organizado por mês e ano (salários, extras, freelances, bônus).
- **Motor de Sobra Livre & Painel Analítico:** Indicadores de saúde orçamentária (`POSITIVO`, `LIMITE`, `DÉFICIT`), saldo previsto versus caixa atual, gráfico de distribuição por categoria e projeção automática de despesas fixas na virada do mês.

### 🎨 4. Interface Fintech v2.0 & Responsividade
- **App Shell Moderno:** Navegação lateral fixa no desktop (*drawer* no mobile), alternância **Modo Escuro / Modo Claro**, tipografia otimizada para valores numéricos e contraste acessível (WCAG 2.2 AA).
- **Mobile-First:** Tabelas adaptadas para visualização em cartões em telas pequenas e modais acessíveis com navegação por teclado.

---

## 📖 Como Utilizar o Sistema

1. **Criar Conta e Confirmar E-mail:** Cadastre-se com uma senha forte e valide o código de verificação enviado ao seu e-mail.
2. **Registrar Receitas e Despesas:** No **Painel Financeiro**, selecione o mês desejado, cadastre suas receitas e contas e marque cada despesa como paga conforme quitar.
3. **Configurar Família (Opcional):** Em **Minha Conta**, envie convites para familiares, aceite convites pendentes e ajuste as opções de compartilhamento de renda e despesas.
4. **Reforçar a Segurança:** Ative a **Verificação em 2 Fatores (2FA)** e cadastre canais de recuperação em **Minha Conta**.

---

## 🛠️ Stack Tecnológica

| Camada | Tecnologias |
| :--- | :--- |
| **Linguagem & Runtime** | Java 25 |
| **Framework Backend** | Spring Boot 4 (WebMVC, Validation, Security, Scheduling, Mail) |
| **Banco de Dados** | PostgreSQL + Spring Data JPA / Hibernate + Flyway |
| **Frontend** | HTML5 Semântico, CSS3 Modular, JavaScript ES Modules & Canvas API |
| **Testes** | JUnit 5, Mockito, AssertJ e Spring `MockMvc` |
| **Infraestrutura** | Docker (Multi-Stage Build) & Docker Compose |

---

## 💻 Como Rodar Localmente

### Opção 1: Com Docker Compose
1. Crie um arquivo `.env` na raiz do projeto com suas variáveis locais (consulte as chaves esperadas em `application.properties`, definindo uma `JWT_SECRET` forte de pelo menos 32 caracteres).
2. Suba o ambiente:
   ```bash
   docker compose up --build
   ```
3. Acesse **`http://localhost:8080`**.

### Opção 2: Com Maven Local
1. Configure o banco PostgreSQL e o arquivo `.env` local (nunca versione arquivos `.env`).
2. Execute:
   ```bash
   ./mvnw spring-boot:run
   ```
3. Acesse **`http://localhost:8080`**.

---

## 🧪 Testes Automatizados

O projeto conta com **123 testes automatizados** cobrindo domínio, regras financeiras, módulo família, validações, segurança e controladores:

```bash
./mvnw test -Dtest="*Test"
```

---

## 👤 Autor

Desenvolvido por **Marcos Paulo de Freitas Pereira** — Estudante de Engenharia de Software e Desenvolvedor Backend Java.

- **LinkedIn:** [linkedin.com/in/marcos-paulo7](https://www.linkedin.com/in/marcos-paulo7)
- **Instagram:** [@__.kiin](https://www.instagram.com/__.kiin)
- **GitHub:** [github.com/Marcos-Dev7](https://github.com/Marcos-Dev7)
