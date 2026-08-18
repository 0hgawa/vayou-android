# Auditoria de UX — o novo contra o original

A instrução do projeto é **reescrever otimizado sem mudar a UX**. Esta é a lista do que eu mudei
sem ser pedido, levantada abrindo os arquivos do original que eu deveria ter aberto antes de
escrever.

Cada linha é uma divergência a corrigir, não uma escolha a defender.

---

## Biblioteca

Original: `mobile/feature-videopicker/.../mediapicker/MediaPickerScreen.kt` — **984 linhas**
Meu: `feature/library/.../LibraryScreen.kt` — ~150 linhas

Nunca abri o arquivo do original. Escrevi a tela do zero.

| | original | meu |
|---|---|---|
| Título da barra | "Video" | "Library" |
| Busca | botão na barra | **ausente** |
| Adicionar | botão na barra | **ausente** |
| Seleção múltipla | barra contextual: tocar, compartilhar, excluir, marcar/desmarcar tudo | **ausente** |
| Abas | Pastas · Vídeos · **Listas** | Pastas · Vídeos |
| Recentes | seção própria no topo | **ausente** |
| Tocar tudo | botão | **ausente** |
| Layout | lista **ou grade** (`LazyGrid`) | só lista |
| Excluir | diálogo com contagem e aviso | **ausente** |
| Renomear / nova lista | diálogos | **ausente** |
| Ordenação | folha inferior | ✅ copiado |

## Barra inferior

Original: `core/ui/.../VayouNavBar.kt` (86 linhas) e `VayouNavRail.kt` (97), montadas em
`MainActivity.kt` linhas 151-220.

| | original | meu |
|---|---|---|
| Barra | Vídeo · Áudio · Rede · Ajustes | ✅ igual |
| Trilho em janela larga | sim, a partir de Medium | ✅ igual |
| Seleção | ícone preenchido, mesma cor | ✅ igual |
| Estado por aba | preservado ao trocar | ✅ igual (`SaveableStateHolder`) |
| Rede | tela própria | ✅ portada |
| Áudio · Ajustes | telas próprias | estado vazio: as fases 5 e 7 ainda não chegaram |

## Rede

Original: `core/smb` (14 ficheiros, 1217 linhas), `NetworkBrowserScreen.kt` (1696) e
`NetworkBrowserViewModel.kt` (441).

| | original | meu |
|---|---|---|
| Abas | Servidores · Canais, num pager | ✅ igual |
| Descoberta | mDNS e varredura da sub-rede em paralelo | ✅ igual |
| Ligação | credenciais guardadas, depois convidado, depois pede | ✅ igual |
| Partilhas | listadas por DCERPC, ocultas fora | ✅ igual |
| Navegador | migalhas, ordenação, procura, fixar pasta | ✅ igual |
| Reprodução | `smb://` pelo mesmo DataSource, legendas ao lado | ✅ igual |
| Canais | M3U, agrupado, procura, estrela, filtro de grupo, país | ✅ igual |
| Semente | iptv-org do país do telemóvel na primeira execução | ✅ igual |
| Áudio de uma partilha | player de música próprio | abre no player de vídeo — a fase 5 ainda não chegou |

## Player — controles

Original: `ControlsTopView.kt`, `ControlsBottomView.kt`
Corrigido depois de reclamação, não antes.

| | original | meu |
|---|---|---|
| Barra superior | voltar · título rolante · ⋮ | ✅ igual (após correção) |
| ⋮ do topo | temporizador, segundo plano, modo noturno, equalizador | temporizador e equalizador |
| Transporte | anterior · play · próximo | ✅ igual |
| Rodapé | legenda, áudio, velocidade, ⋯ com trava/rotação/enquadramento/AB/PiP | tudo solto numa fileira rolável, sem ⋯ |
| Atraso de legenda | steppers no seletor | **ausente** |
| PiP | botão | só automático ao sair |
| Lista de reprodução | botão | **ausente** |

## Player — folha de legendas

Original: `SubtitleSelectorView.kt` — 340 linhas. Não abri antes de escrever.
Corrigido depois de reclamação.

| | original | meu |
|---|---|---|
| Estrutura | faixas ✓ · divisor · ações · divisor · atraso | ✅ igual (após correção) |
| Atraso | linha com steppers | **ainda ausente** |

## Player — customização de legenda

Original: `SubtitleStyleView.kt` — 798 linhas. Não abri antes de escrever.
Presets adicionados depois de reclamação; o resto ainda diverge.

| | original | meu |
|---|---|---|
| Abas | Predefinições · Personalizar | ✅ igual (após correção) |
| 6 estilos + 3 tamanhos | tiles | ✅ copiado |
| Fonte | escolha de fonte | **ausente** |
| Cor do contorno | escolha | **ausente** |
| Sombra | interruptor próprio | dobrado no contorno |
| Cor do texto | seletor completo | 6 amostras |

## Modais

| | original | meu |
|---|---|---|
| Seletores do player | folha inferior | eram diálogos centrais — ✅ corrigido |
| Erro | diálogo | ✅ igual |

## Gestos

| | original | meu |
|---|---|---|
| Pinça | só zoom, centralizado (`enablePanGesture = false`) | arrastava junto — ✅ corrigido |
| Segurar 2× | desligado por padrão | não implementado — correto |

---

## O que fazer

Ordem por peso do que o usuário perde:

1. Biblioteca: seleção múltipla, busca, recentes, grade, listas de reprodução
2. Rodapé do player: agrupar em ⋯ como o original
3. Atraso de legenda
4. ⋮ do topo: segundo plano e modo noturno (o equalizador já está)
5. Customização: fonte, cor do contorno, sombra separada

## Regra, daqui em diante

Antes de escrever qualquer arquivo com equivalente no original, abrir o equivalente e **dizer na
mensagem qual arquivo foi lido e quantas linhas**. Divergência proposta se declara **antes** de
escrever. Se não couber na sessão, dizer isso em vez de improvisar.
