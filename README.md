<div align="center">
  <img src="src/main/resources/static/logo.png" alt="FinancialFamily Logo" width="96" style="border-radius: 22px;" />

  # FinancialFamily — Gestão Financeira & Patrimonial Familiar

  **Versão 1.2.0** &bull; Plataforma Full-Stack de Governança Financeira Familiar, Membros & Cálculo de Sobra Livre

  [![Version](https://img.shields.io/badge/version-1.2.0-10b981?style=for-the-badge)](https://github.com/Marcos-Dev7/financas-investimentos)
  [![Java](https://img.shields.io/badge/Java-25-f59e0b?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
  [![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.3-6db33f?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
  [![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Neon_Cloud-336791?style=for-the-badge&logo=postgresql&logoColor=white)](https://neon.tech/)
  [![Tests](https://img.shields.io/badge/Tests-92_Passing-10b981?style=for-the-badge&logo=junit5&logoColor=white)](#%EF%B8%8F-testes-automatizados)

  🌐 **Acesse a Plataforma Online:** [https://financial-family.onrender.com](https://financial-family.onrender.com)
</div>

---

## 📌 Sobre o Projeto

**FinancialFamily** é uma plataforma web completa (API REST + Interface Web Responsiva Desktop & Mobile) desenvolvida para o controle financeiro familiar, focada na clareza de gastos, gestão compartilhada de caixa e construção de patrimônio.

O sistema resolve um problema clássico do orçamento doméstico: separar o que é **conta prevista (Despesa Bruta)** do que já foi **efetivamente quitado (Despesa Paga)**, revelando em tempo real a verdadeira **Sobra Livre** mensal — tanto no perfil individual quanto na **Visão Consolidada da Família**.

---

## ✨ Funcionalidades Principais (`v1.2.0`)

### 👨‍👩‍👧‍👦 1. Módulo Família & Membros (Novo na `v1.1.0`)
- **Convites para Múltiplos Membros (E-mail ou Link com Token 48h):** O titular pode convidar seu cônjuge ou familiares por e-mail ou link (`?convite=TOKEN`) com prazo de expiração de 48 horas.
- **Visão Consolidada de Despesas Familiares (Modo Somente Leitura):** Painel dedicado que reúne as despesas da família identificando o **Responsável** por cada lançamento. Membros apenas visualizam — **nunca podem editar ou apagar dados de outra pessoa** (garantido no backend).
- **Compartilhamento Opcional de Renda (*Opt-in / Opt-out*):** A renda de cada membro só entra na soma da **Renda Familiar e Sobra Livre Consolidada** se a própria pessoa ativar o interruptor de compartilhamento, podendo desativar a qualquer momento.
- **Revogação Imediata de Acesso:** O titular pode remover qualquer membro com 1 clique, cortando o acesso imediatamente no banco de dados. Usuários fora da família são bloqueados no backend com `403 Forbidden`.

### 🔐 2. Autenticação, Recuperação de Senha, Cookies & Isolamento de Dados
- **Cadastro com Política de Senha Forte:** Validação em tempo real (mínimo 8 caracteres, maiúscula, minúscula, número e caractere especial) + confirmação obrigatória de senha.
- **Verificação de E-mail com Código de 6 Dígitos:** Envio de código numérico temporário (`SecureRandom`, válido por 15 minutos) para ativação da conta.
- **Recuperação de Senha ("Esqueci minha senha"):** Fluxo completo com envio de link (`?resetToken=...`, válido por 30 min) por **E-mail Principal** ou **E-mail de Recuperação**, ou **código numérico de 6 dígitos via SMS** (Brevo Transactional SMS API, válido por 15 min, com fallback automático para o e-mail vinculado se não houver créditos SMS ativos).
- **Sessão por Aba & Gestão de Cookies:** Por padrão a sessão é isolada por aba (`sessionStorage`), impedindo login automático ao abrir novas páginas. Inclui banner e modal de **Preferências de Cookies** (`Apenas Essenciais` ou `Aceitar Todos`) e opção explícita *"Manter conectado"* no login.
- **Sessão Stateless JWT & Isolamento Multi-Usuário:** Cada conta possui ambiente totalmente isolado — um usuário gerencia exclusivamente suas próprias receitas e despesas.

### 👤 3. Área do Usuário ("Minha Conta")
- **Upload de Foto de Perfil:** Recorte quadrado e compressão automática (`256x256`) no navegador, com persistência direta no banco de dados na nuvem.
- **Gestão de Dados Cadastrais, Recuperação & Segurança:** Atualização de nome completo, e-mail principal, **telefone** e **e-mail de recuperação**, alteração de senha, preferências de cookies e opção de exclusão permanente da conta.

### 💰 4. Gestão de Despesas, Receitas e Motor de Sobra Livre
- **Despesas Mensais:** Cadastro, edição, exclusão, paginação, busca por descrição/categoria, baixa de pagamento com 1 clique (`Pagar`) e reversão (`Estornar`).
- **Saldos e Receitas:** Registro de múltiplas fontes de renda por mês/ano (Salários, Freelances, Bônus).
- **Motor de Cálculo de Sobra Livre & Painel Analítico:** Calcula automaticamente a **Sobra Livre (Prevista)** (`Receita Total - Despesas Brutas`) para sinalizar imediatamente `POSITIVO`, `LIMITE` ou `DEFICIT` antes mesmo do vencimento das contas, mantendo visível o caixa atual (`Receita - Despesas Pagas`), a distribuição por categoria e o termômetro de comprometimento com alerta visual crítico.
- **Agendador Automático (`@Scheduled`):** Projeta automaticamente todas as despesas fixas para o mês seguinte na virada de cada dia 1º.

### ⚡ 5. Controle de Carga, Rate Limiting, Acessibilidade & Mobile-First
- **Rate Limiting por IP (`RateLimitFilter`):** Limite de 15 requisições/minuto por IP nas rotas de autenticação e 120 requisições/minuto na API (`HTTP 429 Too Many Requests`).
- **Pool `HikariCP` Serverless & Controle de RAM da JVM:** Pool otimizado para *scale-to-zero* na Neon Cloud, índices compostos no PostgreSQL e teto de memória (`-XX:MaxRAMPercentage=75.0`) no container Docker.
- **Design System Sobriedade Financeira & WCAG 2.2 AA:** Paleta escura de alto contraste (≥ 4.5:1 em textos e ≥ 3:1 em controles), numerais tabulares (`tabular-nums`), foco visível (`:focus-visible`), navegação por teclado com *focus trap* em modais, confirmações via diálogo acessível (`role="alertdialog"`) para ações destrutivas, *landmarks* semânticos e suporte a `prefers-reduced-motion`.
- **Layout Responsivo Mobile-First:** Menu compacto no mobile, alvos de toque de no mínimo `44×44px` e tabelas convertidas em cards sem rolagem horizontal a partir de `320px`.

---

## 📖 Como Utilizar o Sistema (Guia do Usuário)

1. **Criar sua Conta e Validar E-mail:**
   - Acesse a plataforma, clique em **"Criar Conta"**, preencha seus dados seguindo o indicador de senha forte e confirme sua conta com o **código de 6 dígitos** recebido por e-mail.
2. **Cadastrar Receitas e Despesas Individuais:**
   - No **Painel Financeiro**, selecione o mês no topo, adicione suas fontes de renda em **`+ Nova Receita`** e suas contas em **`+ Nova Despesa`**.
   - Clique em **`Pagar`** conforme quitar cada conta no mês para atualizar sua **Sobra Livre Disponível**.
3. **Convidar Familiares e Usar a Visão Consolidada ("Família"):**
   - Acesse **"Minha Conta"** → **"Configurações da Família & Membros"** para enviar convites por e-mail (ou copiar o link/código de 48h), aceitar um convite via token e gerenciar os membros ativos ou o compartilhamento de renda (**"Incluir minha renda na soma familiar"**).
   - Na aba **"Família"** do menu superior, você acessa diretamente no topo o **Resumo Consolidado da Família** e a tabela de **Despesas Familiares do Mês** (em modo somente leitura, além de alertas de convites pendentes recebidos quando houver).
4. **Personalizar seu Perfil ("Minha Conta"):**
   - Na aba **"Minha Conta"**, envie sua **foto de perfil**, atualize seu nome/e-mail ou altere sua senha.

---

## 🛠️ Stack Tecnológica

| Camada | Tecnologias |
| :--- | :--- |
| **Linguagem & Runtime** | Java 25 |
| **Framework Backend** | Spring Boot 4.0.3 (WebMVC, Validation, Scheduling, Mail) |
| **Segurança & Carga** | Spring Security 6 + Token JWT + Rate Limiting Filter por IP |
| **Banco de Dados & Migrations** | PostgreSQL (Neon Serverless) + Spring Data JPA / Hibernate + Flyway (`V1` a `V10`) |
| **Frontend** | HTML5 Semântico, CSS3 Design System com Tokens (Sobriedade Financeira & WCAG 2.2 AA), Vanilla JS ES6+ & Canvas API |
| **Testes** | JUnit 5, Mockito, AssertJ e Spring `MockMvc` (87 testes automatizados) |
| **Infraestrutura & Deploy** | Docker (Multi-Stage Build), Docker Compose, Render & Neon Cloud |

---

## 🔌 Principais Endpoints da API REST

### Família & Membros (`/familia`)
| Método | Rota | Descrição | Autenticação |
| :--- | :--- | :--- | :---: |
| `GET` | `/familia` | Retorna membros ativos, convites e status de renda | `Bearer JWT` |
| `POST` | `/familia/convites` | Gera convite familiar com token válido por 48h | `Bearer JWT` |
| `POST` | `/familia/convites/aceitar` | Aceita convite validando token, prazo e e-mail | `Bearer JWT` |
| `PATCH` | `/familia/compartilhar-renda` | Ativa ou desativa o compartilhamento da própria renda | `Bearer JWT` |
| `DELETE` | `/familia/membros/{vinculoId}` | Remove membro da família (corta acesso imediato) | `Bearer JWT` |
| `GET` | `/familia/despesas` | Lista despesas familiares consolidadas (somente leitura) | `Bearer JWT` |
| `GET` | `/familia/resumo/{mes}` | Calcula receita somada, gastos e Sobra Livre Familiar | `Bearer JWT` |

### Autenticação & Conta (`/auth`)
| Método | Rota | Descrição | Autenticação |
| :--- | :--- | :--- | :---: |
| `POST` | `/auth/registrar` | Cadastra usuário e envia código de 6 dígitos | Pública |
| `POST` | `/auth/verificar-email` | Valida o código de 6 dígitos e retorna Token JWT | Pública |
| `POST` | `/auth/reenviar-codigo` | Reenvia novo código de verificação | Pública |
| `POST` | `/auth/login` | Autentica e-mail/senha e retorna Token JWT | Pública |
| `POST` | `/auth/esqueci-senha` | Envia link de recuperação via e-mail, e-mail de recuperação ou telefone | Pública |
| `POST` | `/auth/redefinir-senha` | Redefine a senha validando o token de recuperação (30 min) | Pública |
| `GET` | `/auth/me` | Retorna dados, contatos de recuperação e foto do usuário autenticado | `Bearer JWT` |
| `PUT` | `/auth/perfil` | Atualiza nome, e-mail, telefone, e-mail de recuperação e foto | `Bearer JWT` |
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

O projeto conta com **87 testes automatizados** cobrindo Entidades de Domínio, Enums, Conversores, Validador de Senha Forte, Rate Limiting, Módulo Família, Services (Mockito) e Controllers (MockMvc). Para executá-los:

```bash
./mvnw test -Dtest="*Test"
```

---

## 👤 Autor

Desenvolvido por **Marcos Paulo de Freitas Pereira** — Estudante de Engenharia de Software e Desenvolvedor Backend Java.

- **LinkedIn:** [linkedin.com/in/marcos-paulo7](https://www.linkedin.com/in/marcos-paulo7)
- **Instagram:** [@__.kiin](https://www.instagram.com/__.kiin)
- **GitHub:** [github.com/Marcos-Dev7](https://github.com/Marcos-Dev7)
