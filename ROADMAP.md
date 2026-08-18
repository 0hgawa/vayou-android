# Vayou — reconstrução

O projeto anterior está em `D:\Apps\vayou-android` e continua intacto. Ele é a
**especificação**: cada comportamento que não se explica lendo o código novo, lê-se no antigo.
Backup verificado em `D:\Apps\vayou-android-backup-2026-07-30`.

Regras que valem para todas as fases:

1. **Uma fase por vez.** Nenhuma começa antes de a anterior compilar, passar no ktlint e rodar.
2. **Copiar em vez de redigitar** o que é dado e não código: migrações do Room, traduções,
   ProGuard rules, `libs.versions.toml`.
3. **Só entra o que é usado.** Um arquivo só é trazido quando alguma coisa o chama.
4. **Nada de `TODO`.** Se não dá para terminar, não começa.
5. Tudo em [CONVENTIONS.md](CONVENTIONS.md) vale desde o primeiro commit.

---

## Fase 0 — Fundação ✅

- [x] Wrapper, catálogo de versões, `.editorconfig`, `.gitignore` copiados
- [x] `build-logic` com 4 convention plugins — os 17 módulos deixam de repetir configuração
- [x] `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties` com cache e R não-transitivo
- [x] `app` mínimo: abre, Hilt de pé, edge-to-edge
- [x] `./gradlew :app:assembleDebug` e `ktlintCheck` verdes
- [x] `applicationId` com sufixo `.next` no debug, para conviver com o app atual no aparelho

## Fase 1 — Design system ✅

O coração visual. Vem antes das telas porque toda tela depende dele.

- [x] `core:ui` — paleta, tipografia, formas, espaçamento, tamanhos de ícone
- [x] Sistema de cores já corrigido: `m3Scheme` memoizado, `VayouColors` como `data class`,
      `accentFixed` nomeado, sem `divider` nem `disabled`
- [x] Conflito do accent resolvido: `accent` virou papel tonal, `accentFixed` guarda a marca,
      e `accentOnBackground` caiu por ser duplicata
- [ ] Componentes, **só quando o primeiro consumidor aparecer**
- [ ] Ícones: só os Phosphor efetivamente usados

## Fase 2 — Dados ✅

- [x] `core:model` — Kotlin puro, sem Android. 11 dos 25 tipos; o resto vem com sua fase
- [x] `core:database` — esquema v8 e as 7 migrações copiados literalmente, com o schema exportado
- [x] `core:datastore` — só o store de preferências do app; os outros 3 vêm com suas fases
- [x] `core:common` — dispatchers, escopo e logger; o resto não veio
- [ ] `core:data` — repositórios (vem com a Fase 3, junto do primeiro consumidor)

## Fase 3 — Biblioteca local ✅

Primeiro marco com utilidade real: dá para instalar e usar.

- [x] `core:media` — scan e sincronização
- [x] `core:data` e `core:domain` — repositórios e ordenação
- [x] `core:imageloader` — miniatura a partir de um frame do arquivo
- [x] `feature:library` — pastas e vídeos, com permissão de armazenamento
- [x] **Ponto de verificação:** abre a lista e mostra os vídeos do aparelho
- [x] Ordenação pela interface — folha com os cinco eixos; tocar no ativo inverte a ordem
- [ ] Seleção múltipla (vem quando houver uma ação para aplicar à seleção)

## Fase 4 — Player de vídeo

A maior peça isolada.

Feito e verificado no aparelho:

- [x] `core:player` — Media3, extensão FFmpeg, decoders por software
- [x] Superfície, play/pause, barra de progresso, arrastar para buscar
- [x] Brilho e volume por arraste, com leitura na tela
- [x] Título, voltar, pular ±10s (gesto e botão), trava de tela
- [x] Faixas de áudio, legendas embutidas, velocidade
- [x] Janela flutuante (PiP)

**Falta — levantado comparando arquivo a arquivo com o original.** O player antigo tem 18
estados e 18 telas; o novo tem o equivalente a 6. Em ordem de peso:

- [x] **Retomar de onde parou** — salvo a cada pausa e ao sair da tela, zerado se o filme
      chegou ao fim. Acendeu de quebra a barra de progresso das miniaturas.
- [x] **Abrir vídeo de outro app** — três `intent-filter`, mais os extras que outros apps já
      mandam: título, posição, legendas, lista e o resultado de volta
- [x] **Fila** — a pasta na ordem que a biblioteca mostra, com anterior/próximo e posição por arquivo
- [x] **Esconder os controles sozinho** — 4s, e só enquanto está tocando
- [x] **Estado de erro** — diálogo com causa em linguagem humana, tentar de novo ou sair
- [x] **Enquadramento** — ajustar, esticar, cortar, 100%, com o nome na pílula
- [x] **Pinça e arraste** — ¼ a 4×, zoom guardado por arquivo *(não verificado: dois dedos não
      são injetáveis por adb sem root)*
- [x] **Rotação** — segue o aparelho mesmo com a trava do sistema; botão fixa a outra orientação
- [x] Legenda externa (`.srt` de fora), com conversão de codificação para UTF-8
- [ ] Segurar para 2× — o original entrega **desligado** (`useLongPressControls = false`), então vai
      com a preferência na Fase 7
- [ ] Repetição AB
- [x] Temporizador de sono — contagem no serviço, sobrevive à tela fechar
- [ ] Equalizador, reforço de graves, virtualizador, aumento de volume — todos no serviço
- [ ] Modo noturno — **compressão de faixa dinâmica no áudio**, não escurecer a tela (precisa do serviço)
- [x] Serviço de mídia com sessão — notificação, botões de fone, tela de bloqueio
- [ ] Tocar em segundo plano (preferência), foco de áudio, pausar ao tirar o fone
- [ ] Idioma preferido de áudio e de legenda
- [ ] Estilo de legenda (tamanho, cor, fonte, contorno, fundo, posição) — Fase 7
- [ ] Busca de legenda on-line e tradução em tempo real — Fase 7
- [ ] Prioridade de decodificador — Fase 7

## Fase 5 — Áudio

- [ ] `feature:music` — biblioteca, player, fila, tags
- [ ] Módulo próprio desde o início: no projeto antigo eram ~3.000 linhas soltas dentro de `app`

## Fase 6 — Rede

- [ ] `core:smb` — sem a string de UI que vivia ali
- [ ] `feature:network` — navegador, IPTV, favoritos

## Fase 7 — Extras

- [ ] Cast
- [ ] Equalizador, sleep timer, AB repeat
- [ ] `feature:settings`
- [ ] Traduções: **copiar os 12 idiomas**, nunca redigitar

## Fase 8 — TV

- [ ] `app-tv` e os módulos `tv:*`
- [ ] Com `stringResource` desde o começo — o app de TV antigo tinha 21 strings chumbadas

## Fase 9 — Performance, medida

- [ ] Baseline Profile — não existia no projeto antigo, e é o maior ganho de partida disponível
- [ ] Compose stability report (`-Pvayou.composeReports`) e atacar as classes instáveis
- [ ] Configuration cache
- [ ] Comparar tamanho de APK e tempo de partida com o projeto antigo

## Fase 10 — Corte

- [ ] Os 5 testes de fumaça passando nos dois: vídeo local, vídeo em SMB, áudio, cast, PiP
- [ ] Só então remover o sufixo `.next`
- [ ] Só então arquivar o projeto antigo

---

## Onde estamos

**Fases 0 a 3 concluídas. A Fase 4 está adiantada.**

O app lê a biblioteca, é o app com que se abre um vídeo, retoma de onde parou, segue para o próximo
da pasta, enquadra de quatro jeitos, gira, aceita legenda de fora e diz o que houve quando um
arquivo falha.

O que resta da Fase 4 é o que o original também guarda atrás de preferências ou de um serviço:
repetição AB, temporizador, equalizador, modo noturno e segundo plano. Nenhum deles bloqueia a
Fase 5.
