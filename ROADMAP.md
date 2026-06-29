# Vayou Android — Roadmap de Refatoracao

> Documento vivo. Cada fase deve ser concluida e compilar antes de avancar para a proxima.
> Regra absoluta: nenhuma fase pode piorar performance. Cada mudanca deve ser neutra ou positiva.

---

## Visao Geral

```
FASE 1 — Reestruturacao de modulos (mover arquivos, atualizar Gradle)
FASE 2 — Correcoes de performance (recomposicao, alocacoes, keys)
FASE 3 — Limpeza de codigo (dead code, TODOs, redundancia)
FASE 4 — Internacionalizacao (hardcoded strings → resources)
FASE 5 — Preparacao TV (app-tv shell, modulos tv/)
```

---

## FASE 1 — Reestruturacao de Modulos

**Objetivo:** Mover o codigo mobile-specific de `feature/` e `core/ui` para `mobile/`, deixando `core/` puramente compartilhado.

### 1.1 Criar estrutura de pastas

```
mobile/
├── ui/                        ← mover de core/ui (tema M3, componentes, designsystem)
├── feature-player/            ← mover de feature/player
├── feature-videopicker/       ← mover de feature/videopicker
├── feature-settings/          ← mover de feature/settings
└── feature-network/           ← novo (futuro)
```

### 1.2 Separar core/ui

**O que fica em `core/ui` (compartilhado):**
- `base/DataState.kt` — sealed class generico para estados async
- `base/ScreenState.kt` — sealed class generico para UI state
- `extensions/PaddingValues.kt` — extension de utilidade
- `res/values*/strings.xml` — recursos de string (compartilhados)

**O que move para `mobile/ui`:**
- `designsystem/theme/` — VayouColors, VayouTypography, VayouShapes, VayouMotion, VayouSpacing, VayouTheme, VayouDynamicColors
- `designsystem/components/` — Todos os 19 componentes Vayou* (VayouButton, VayouBottomSheet, VayouNavBar, etc.)
- `components/` — PreferenceItem, PlaylistThumbnail, RenameDialog, ListItemComponent, etc.
- `composables/` — PermissionDetailView, PermissionMissingView, PermissionRationaleDialog
- `preview/` — utilidades de preview
- `theme/Theme.kt` — tema legado (DELETAR apos confirmar que nao e usado)

### 1.3 Mover feature modules

| De | Para | Namespace novo |
|----|------|----------------|
| `feature/player` | `mobile/feature-player` | `dev.vayou.mobile.feature.player` |
| `feature/settings` | `mobile/feature-settings` | `dev.vayou.mobile.feature.settings` |
| `feature/videopicker` | `mobile/feature-videopicker` | `dev.vayou.mobile.feature.videopicker` |
| `core/ui` (mobile parts) | `mobile/ui` | `dev.vayou.mobile.ui` |

**Nota:** `feature/settings` usa namespace inconsistente `dev.vayou.settings` — padronizar para `dev.vayou.mobile.feature.settings`.

### 1.4 Atualizar dependencias Gradle

**settings.gradle.kts:**
```kotlin
// Remover
include(":feature:player")
include(":feature:settings")
include(":feature:videopicker")

// Adicionar
include(":mobile:ui")
include(":mobile:feature-player")
include(":mobile:feature-settings")
include(":mobile:feature-videopicker")
```

**app/build.gradle.kts:**
```kotlin
// Remover
implementation(project(":feature:player"))
implementation(project(":feature:settings"))
implementation(project(":feature:videopicker"))

// Adicionar
implementation(project(":mobile:ui"))
implementation(project(":mobile:feature-player"))
implementation(project(":mobile:feature-settings"))
implementation(project(":mobile:feature-videopicker"))
```

### 1.5 Atualizar imports no app/

**Arquivos afetados:**
- `app/.../MainActivity.kt` — 2 imports de feature.player
- `app/.../navigation/MediaNavGraph.kt` — 10+ imports de feature.player e feature.videopicker
- `app/.../navigation/SettingsNavGraph.kt` — 20+ imports de settings.*
- `app/.../playlist/PlaylistScreen.kt` — imports de core.ui components
- `app/.../playlist/PlaylistDetailScreen.kt` — imports de core.ui components

### 1.6 Deletar pastas vazias

- Remover `feature/` inteiro apos migrar tudo
- Remover arquivos mobile-specific de `core/ui/` apos migrar

### 1.7 Validacao

- [ ] `./gradlew assembleDebug` compila sem erros
- [ ] Instalar no celular e testar navegacao completa
- [ ] Verificar que nenhum import quebrou

---

## FASE 2 — Correcoes de Performance

**Objetivo:** Corrigir todos os problemas de recomposicao e alocacao desnecessaria. Zero regressao.

### 2.1 CRITICO — Adicionar keys em LazyColumn/LazyRow

Sem `key`, o Compose recria todos os items quando a lista muda. Com `key`, so recria o que mudou.

| Arquivo | Linha | Fix |
|---------|-------|-----|
| `SubtitlePreferencesScreen.kt` | 182, 200, 218 | `key = { it.second }`, `key = { it.name }`, `key = { it }` |
| `FolderPreferencesScreen.kt` | 89 | `key = { _, folder -> folder.path }` |
| `CastButton.kt` | 222 | `key = { it.name }` + cachear `routes.distinctBy` em `remember` |
| `PlayerPreferencesScreen.kt` | 199, 217, 235 | `key = { it.name }` para todos os `items(*.entries)` |
| `AppearancePreferencesScreen.kt` | 109 | `key = { it.name }` |
| `GesturePreferencesScreen.kt` | 234 | `key = { it.name }` |
| `DecoderPreferencesScreen.kt` | 98 | `key = { it.name }` |
| `AudioPreferencesScreen.kt` | 128 | `key = { it.second }` |

### 2.2 CRITICO — VayouTabRow mutableStateList

**Problema:** `mutableStateListOf` recompoe todas as tabs quando qualquer tab muda de largura.
**Fix:** Usar `IntArray` sem state tracking, ou `derivedStateOf` so para o indicador.

```kotlin
// ANTES (recompoe tudo)
val tabWidths = remember { mutableStateListOf(*Array(tabs.size) { 0 }) }

// DEPOIS (recompoe so o indicador)
val tabWidths = remember { IntArray(tabs.size) }
var indicatorOffset by remember { mutableStateOf(0f) }
var indicatorWidth by remember { mutableStateOf(0) }
```

### 2.3 ALTO — ImageRequest sem remember

**Arquivo:** `VideoItem.kt:269-273`

```kotlin
// ANTES (reconstroi a cada frame)
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(video.uriString).crossfade(true).build(),
    ...
)

// DEPOIS
val imageRequest = remember(video.uriString) {
    ImageRequest.Builder(context)
        .data(video.uriString).crossfade(true).build()
}
AsyncImage(model = imageRequest, ...)
```

### 2.4 ALTO — PaddingValues alocados em hot paths

**Arquivos:** `VideoItem.kt:141`, `FolderItem.kt:108`, `MediaView.kt:97`

```kotlin
// ANTES (nova alocacao por item, por frame)
contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

// DEPOIS (uma alocacao, reutilizada)
val itemPadding = remember { PaddingValues(horizontal = 16.dp, vertical = 8.dp) }
```

Para `MediaView.kt:97`:
```kotlin
val computedPadding = remember(contentPadding, contentHorizontalPadding) {
    contentPadding + PaddingValues(horizontal = contentHorizontalPadding, vertical = 8.dp)
}
```

### 2.5 ALTO — Lambdas sem remember em LazyColumn items

**Arquivos:** `VideoItem.kt:154-164`, `FolderItem.kt:121-123`, `PlaylistScreen.kt:116`

Lambdas passadas como parametro em items de LazyColumn sao objetos novos a cada recomposicao. Usar `remember` ou extrair callbacks.

### 2.6 MEDIO — Colecoes ineficientes

**SearchMediaUseCase.kt:51-59** — Usar `asSequence()`:
```kotlin
val scoredFolders = folders.asSequence()
    .mapNotNull { folder ->
        val score = searchMatcher.calculateScore(folder.name, folder.path)
        if (score > 0) folder to score else null
    }
    .sortedByDescending { it.second }
    .map { it.first }
    .toList()
```

**SearchMediaUseCase.kt:95** — Cachear `lowercase()`:
```kotlin
fun calculateScore(vararg texts: String): Int {
    val textsLower = texts.map { it.lowercase() }
    return textsLower.maxOfOrNull { calculateTextScore(it) } ?: 0
}
```

**MediaPickerViewModel.kt:60-62** — Usar `mapTo()`:
```kotlin
// ANTES
.map { videos -> videos.map { it.uriString }.toHashSet() }

// DEPOIS
.map { videos -> videos.mapTo(HashSet()) { it.uriString } }
```

### 2.7 MEDIO — CastButton distinctBy recriando lista

**Arquivo:** `CastButton.kt:222`

```kotlin
// ANTES (nova lista a cada recomposicao)
items(routes.distinctBy { it.name }) { route -> ... }

// DEPOIS
val uniqueRoutes = remember(routes) { routes.distinctBy { it.name } }
items(uniqueRoutes, key = { it.name }) { route -> ... }
```

### 2.8 Validacao

- [ ] `./gradlew assembleDebug` compila
- [ ] Testar scroll de lista longa (100+ videos) — deve ser fluido
- [ ] Testar busca com muitos resultados — sem lag
- [ ] Profile com Android Studio Layout Inspector — zero recomposicao desnecessaria

---

## FASE 3 — Limpeza de Codigo

**Objetivo:** Zero dead code, zero redundancia, zero TODOs. Seguir CLAUDE.md a risca.

### 3.1 Remover printStackTrace()

| Arquivo | Linha | Fix |
|---------|-------|-----|
| `LocalMediaSynchronizer.kt` | 161, 179 | Substituir por `Log.e(TAG, "message", e)` ou remover |
| `Context.kt` | 259 | Substituir por logging adequado |
| `File.kt` | 52-60 | Documentar que falha silenciosa e intencional, ou logar |

### 3.2 Resolver TODO

| Arquivo | Linha | Acao |
|---------|-------|------|
| `Context.kt` | 60 | `// TODO handle non-primary volumes` — implementar ou remover com comentario no commit |

### 3.3 Eliminar redundancia

**LocalMediaRepository.kt** — 8 metodos de update seguem padrao identico:
```kotlin
// Padrao repetido 8x:
fun updateX(uri: String, value: T) {
    val entity = dao.get(uri) ?: return
    dao.upsert(entity.copy(x = value))
}
```
Extrair para funcao generica interna:
```kotlin
private suspend fun updateField(uri: String, transform: (MediumEntity) -> MediumEntity) {
    val entity = mediumDao.getByUri(uri) ?: return
    mediumDao.upsert(transform(entity))
}
```

### 3.4 Remover tema legado

**Arquivo:** `core/ui/theme/Theme.kt`
- Verificar se e importado em algum lugar
- Se nao, deletar — o tema real esta em `designsystem/theme/VayouTheme.kt`

### 3.5 Remover parametro default desnecessario

**Arquivo:** `GetSortedVideosUseCase.kt:19`
- `defaultDispatcher` tem default `Dispatchers.Default` mas e injetado via Hilt
- Remover o default para evitar mascarar falha de injecao

### 3.6 Corrigir CoroutineScope leak

**Arquivo:** `LocalMediaService.kt:126-127`
- `renameMediaR` cria `CoroutineScope` sem lifecycle management
- Vincular ao escopo do chamador ou usar `coroutineScope { }` structured concurrency

### 3.7 Validacao

- [ ] `./gradlew assembleDebug` compila
- [ ] `./gradlew ktlintCheck` passa
- [ ] Grep por `TODO`, `FIXME`, `printStackTrace` — zero resultados
- [ ] Instalar e testar funcionalidades afetadas

---

## FASE 4 — Internacionalizacao

**Objetivo:** Zero strings hardcoded no codigo. Tudo via `stringResource()`.

### 4.1 Hardcoded strings a migrar

| Arquivo | Linha | String | Chave sugerida |
|---------|-------|--------|----------------|
| `PictureInPictureState.kt` | 220 | `"skip to previous"` | `R.string.pip_skip_previous` |
| `PictureInPictureState.kt` | 227 | `"pause"` | `R.string.pip_pause` |
| `PictureInPictureState.kt` | 234 | `"play"` | `R.string.pip_play` |
| `PictureInPictureState.kt` | 241 | `"skip to next"` | `R.string.pip_skip_next` |
| `ControlsBottomView.kt` | 323 | `"AB"` | Manter — e label tecnico, nao traduzivel |
| `ControlsBottomView.kt` | 168-173 | `"A"`, `"B"` | Manter — labels tecnicos de AB repeat |
| `DoubleTapIndicator.kt` | 70 | `"seconds"` | `R.string.seconds_abbreviation` |

### 4.2 Adicionar strings ao resources

**values/strings.xml (ingles):**
```xml
<string name="pip_skip_previous">Skip to previous</string>
<string name="pip_pause">Pause</string>
<string name="pip_play">Play</string>
<string name="pip_skip_next">Skip to next</string>
<string name="seconds_abbreviation">seconds</string>
```

**values-pt-rBR/strings.xml (portugues):**
```xml
<string name="pip_skip_previous">Pular para o anterior</string>
<string name="pip_pause">Pausar</string>
<string name="pip_play">Reproduzir</string>
<string name="pip_skip_next">Pular para o proximo</string>
<string name="seconds_abbreviation">segundos</string>
```

### 4.3 Auditar strings existentes

- **Base EN:** 392 keys
- **PT-BR:** 390 keys (99.5% cobertura)
- **Faltando:** apenas `app_name` e `example_url` (marcados `translatable="false"`)
- **Status:** cobertura excelente, nenhuma acao necessaria nos existentes

### 4.4 Auditar outros idiomas

- Verificar se as 43 traducoes existentes cobrem strings novas adicionadas ao Vayou
- Strings novas do Vayou provavelmente nao existem nos outros idiomas — aceitar fallback para ingles

### 4.5 Validacao

- [ ] Grep por strings hardcoded em composables — zero resultados
- [ ] Testar com celular em PT-BR — tudo em portugues
- [ ] Testar com celular em EN — tudo em ingles
- [ ] Testar PiP mode — labels traduzidos

---

## FASE 5 — Preparacao TV

**Objetivo:** Criar a base do app-tv com shell minimo. NAO implementar features ainda.

### 5.1 Criar modulos TV

```
tv/
├── ui/                    ← Tema TV Material, componentes base
├── feature-home/          ← Placeholder
├── feature-browser/       ← Placeholder
├── feature-player/        ← Placeholder
├── feature-settings/      ← Placeholder
├── feature-search/        ← Placeholder
└── feature-smb/           ← Placeholder

app-tv/                    ← APK shell
├── AndroidManifest.xml    ← leanback, banner, landscape
├── build.gradle.kts
└── src/main/java/.../TvMainActivity.kt
```

### 5.2 AndroidManifest do app-tv

```xml
<uses-feature android:name="android.software.leanback" android:required="true" />
<uses-feature android:name="android.hardware.touchscreen" android:required="false" />

<application android:banner="@drawable/tv_banner">
    <activity android:name=".TvMainActivity"
              android:screenOrientation="landscape">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
        </intent-filter>
    </activity>
</application>
```

### 5.3 Dependencias TV

```kotlin
// tv/ui/build.gradle.kts
dependencies {
    implementation("androidx.tv:tv-compose:1.0.0")
    implementation("androidx.tv:tv-material:1.0.0")
    implementation(project(":core:model"))
    implementation(project(":core:common"))
}

// app-tv/build.gradle.kts
dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:media"))
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:domain"))
    implementation(project(":core:smb"))
    implementation(project(":tv:ui"))
}
```

### 5.4 settings.gradle.kts final

```kotlin
rootProject.name = "Vayou"

// App shells
include(":app")
include(":app-tv")

// Core (compartilhado)
include(":core:common")
include(":core:data")
include(":core:database")
include(":core:datastore")
include(":core:domain")
include(":core:media")
include(":core:model")
include(":core:smb")
include(":core:ui")

// Mobile
include(":mobile:ui")
include(":mobile:feature-player")
include(":mobile:feature-settings")
include(":mobile:feature-videopicker")

// TV
include(":tv:ui")
include(":tv:feature-home")
include(":tv:feature-browser")
include(":tv:feature-player")
include(":tv:feature-settings")
include(":tv:feature-search")
include(":tv:feature-smb")
```

### 5.5 Validacao

- [ ] `./gradlew :app:assembleDebug` compila (mobile)
- [ ] `./gradlew :app-tv:assembleDebug` compila (tv shell)
- [ ] Instalar app-tv no emulador Android TV — abre sem crash
- [ ] Mobile continua funcionando 100%

---

## Resumo de Impacto

| Fase | Risco | Impacto Performance | Esforco |
|------|-------|---------------------|---------|
| 1 — Reestruturacao | Medio (muitos imports) | Neutro | Alto |
| 2 — Performance | Baixo (fixes pontuais) | **Positivo** | Medio |
| 3 — Limpeza | Baixo | Neutro/Positivo | Baixo |
| 4 — i18n | Baixo | Neutro | Baixo |
| 5 — TV shell | Baixo (novo, nao quebra) | Neutro | Medio |

## Regras de Execucao

1. **Uma fase por vez.** Nao misturar reestruturacao com fixes de performance.
2. **Compilar apos cada sub-item.** Se quebrar, corrigir antes de avancar.
3. **Instalar e testar apos cada fase.** Nao acumular mudancas sem validacao.
4. **Commit apos cada fase.** Historico limpo e reversivel.
5. **Zero regressao de UX.** Se algo ficou pior visualmente ou em responsividade, e bug.
