# Convenções

Regras que valem para todo arquivo deste repositório. Uma regra só entra aqui se dá para
apontar quando foi quebrada.

## Módulos

```
app/                 shell do celular
app-tv/              shell da TV
core/<nome>/         compartilhado pelos dois shells
feature/<nome>/      uma tela ou um fluxo, do celular
tv/<nome>/           uma tela ou um fluxo, da TV
build-logic/         os convention plugins
```

- `core:` **nunca** depende de `feature:` nem de `app`. A seta aponta sempre para dentro.
- `feature:` nunca depende de outra `feature:`. Se duas precisam da mesma coisa, ela é `core:`.
- Um módulo novo só existe quando dois consumidores precisam dele. Antes disso é um arquivo.

## Nomes de pacote

| Módulo | Namespace |
|---|---|
| `core:<nome>` | `dev.vayou.core.<nome>` |
| `feature:<nome>` | `dev.vayou.feature.<nome>` |
| `tv:<nome>` | `dev.vayou.tv.<nome>` |
| `app` | `dev.vayou` |
| `app-tv` | `dev.vayou.tv` |

Sem exceção. O projeto anterior tinha `dev.vayou.settings` e `dev.vayou.feature.player`
convivendo, e ninguém sabia qual era a regra.

## Nomes de tipo

- **Composable público do design system:** prefixo `Vayou` — `VayouButton`, `VayouChip`.
  O prefixo existe para que um `import` errado do Material 3 salte aos olhos na revisão.
- **Composable de tela:** `<Assunto>Screen` — `MediaPickerScreen`.
- **ViewModel:** `<Assunto>ViewModel`. **Estado:** `<Assunto>UiState`. **Evento:**
  `<Assunto>Event`, com os casos no imperativo — `SelectTheme`, não `OnThemeSelected`.
- **Caso de uso:** `<Verbo><Substantivo>UseCase` — `GetSortedVideosUseCase`.
- **Repositório:** interface `<Assunto>Repository`, implementação `Local<Assunto>Repository`
  ou `Remote<Assunto>Repository`.
- **Booleano:** afirmação — `isVisible`, `hasPermission`, `canPlay`. Nunca `visible`.
- **Constante de arquivo:** `PascalCase` para `Dp`, `Color` e afins; `SCREAMING_SNAKE` só
  para chaves de persistência, onde o valor literal é o contrato.

## Vocabulário

Um termo por conceito, no código inteiro:

| Use | Nunca |
|---|---|
| `media` | `content`, `item` (quando é mídia) |
| `video` / `audio` | `song`, `movie`, `track` |
| `folder` | `directory`, `dir` |
| `artwork` | `cover`, `thumb`, `poster` |
| `pick` / `selected` | `choose`, `checked` (fora de checkbox) |

## Cores, medidas e textos

- **Nenhum literal de cor fora da paleta.** Exceção única: o que é desenhado sobre conteúdo
  que o app não escolheu — um quadro de vídeo, uma capa. Aí a cor é fixa **e comentada**.
- **Nenhum `.dp` solto em componente.** Vem de `VayouTheme.spacing`, `iconSize` ou de uma
  constante nomeada no rodapé do arquivo.
- **Nenhuma string visível no código.** `stringResource`, sempre — inclusive no app de TV.
- Um valor que precisa ser igual em dois lugares vira uma constante. Se um comentário diz
  "igual ao X", é porque deveria ser uma constante.

## Comentários

O código diz **o quê**. O comentário diz **por quê** — e só quando o porquê não é óbvio.

- Comentar a decisão e o que foi descartado, não a mecânica.
- Zero comentário que repita a linha abaixo.
- Zero código comentado. O histórico guarda.
- Zero `TODO` e `FIXME`. Resolve-se agora ou não se mexe.

## Antes de cada commit

- [ ] `./gradlew ktlintCheck` verde
- [ ] `./gradlew :app:assembleDebug` verde
- [ ] Zero import não usado, zero declaração sem chamador
- [ ] Nada aqui violado
