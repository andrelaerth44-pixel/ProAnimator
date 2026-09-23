# Estudo: FlipaClip — Frame Viewer (somente pesquisa, não implementado)

## O que é o FlipaClip

App mobile (Android/iOS) de animação **frame-a-frame** estilo flipbook. Fluxo: desenhar em frames, onion skin, play, exportar filme. Foco em toque/stylus, não em timeline complexa tipo Procreate Dreams.

## O que é o Frame Viewer

Ferramenta de **visão aérea (bird’s-eye)** de todos os frames do projeto — não é o strip fino da timeline inferior.

### Como o usuário abre

- Menu (três pontos) → **Frames Viewer** / **Frame Viewer**
- Ou atalho dedicado em versões recentes

### O que a UI mostra

- **Grade/lista** de thumbnails de cada frame (visão completa da sequência)
- Frame atual destacado
- Long-press / multi-select
- Ações em lote:
  - **Select all**
  - **Copy / Paste** (colar adiante para alongar loops — uso clássico no tutorial da bolinha)
  - **Duplicate** frame(s)
  - **Delete**
  - **Add frame** before / after o selecionado
  - Em alguns tutoriais: **insert between** (inserir entre frames)
  - Jump início / fim
  - Export de frame como imagem (limitado, às vezes 1 por vez)

### Relação com a timeline inferior

| Timeline strip (baixo) | Frame Viewer |
|------------------------|--------------|
| Navegação rápida | Edição estrutural |
| Sempre visível | Tela/modal cheia ou painel grande |
| + / Dup / Del básicos | Multi-select, copy-paste em massa, reordenar |

No FlipaClip, **as mesmas ações** (add left/right, delete, duplicate) também existem no strip — o Viewer é o lugar confortável quando há **muitos** frames.

## Por que existe

1. Projetos longos: strip horizontal fica apertado
2. **Copy all + paste forward** para triplicar um ciclo de 1s → 3s sem redesenhar
3. Reordenar / limpar ranges
4. Inserir in-betweens em massa (botão “add between”)

## É possível no ProAnimator?

**Sim.** Dados já existem: `FlipbookBitmapEngine.frames` + `ImageBitmap` por frame. UI seria:

```
LazyVerticalGrid / LazyRow de thumbnails
+ barra: Select All | Copy | Paste | Dup | Del | + Before | + After
```

APIs necessárias (já parcialmente cobertas):

- `setCurrentFrame`, `addFrame`, `duplicateCurrentFrame`, `deleteCurrentFrame`
- Faltaria explicitamente: **copy range**, **paste at index**, **insert empty between**, **reorder** (move)

## A interface ficaria ocupada / desorganizada?

### Risco se mal feito

ProAnimator **já** tem:

- Toolbar topo (Save, Export, Warp, XF, Audio, …)
- Brush bar
- Easing bar
- OnionControls
- Layers panel
- Bezier editor lateral
- KeyframeTrackStrip
- Timeline strip + mode

Colocar o Frame Viewer **sempre aberto** = **muito ocupado**, sim — especialmente em phone portrait.

### Como fazer sem bagunçar (recomendação)

| Abordagem | Ocupação | Veredicto |
|-----------|----------|-----------|
| Modal / full-screen sheet (como FlipaClip) | Zero no stage enquanto desenha | **Melhor** |
| Painel substitui timeline (toggle “Frames”) | Médio | Bom em tablet |
| Grid permanente sob o canvas | Alta | Evitar |
| Expandir o strip atual com multi-select | Baixa | Meio-termo |

**Padrão FlipaClip:** Viewer é **modo separado** — não compete com o canvas. Volta ao stage com um Close. Isso **não desorganiza** a UI principal.

### Tablet landscape

Side panel (como Layers) só se o usuário pedir “Frames”; default fechado.

## Sobreposição com o que já temos

| Feature ProAnimator | Sobreposição com Frame Viewer |
|---------------------|-------------------------------|
| Strip `F1 F2 F3…` | Navegação simples |
| Dup / Del / +F | Subconjunto das ações |
| Onion | Independente |
| KeyframeTrackStrip | Tracks de transform, não flipbook |

O Frame Viewer **não substitui** keyframes/Perform; complementa o **gerenciamento de frames raster**.

## Conclusão do estudo

| Pergunta | Resposta |
|----------|----------|
| Dá para implementar? | Sim, dados e APIs base prontos |
| UI fica lotada? | Só se for permanente no workspace |
| Como evitar bagunça? | Modal/sheet full-screen OU toggle que troca com o stage |
| Prioridade vs Dreams? | Média — útil para loops longos; Dreams prioriza tracks |
| Implementar agora? | **Não** (pedido do usuário: só estudo) |

Quando for implementar: 1 botão **Frames** → sheet com grid + multi-select + copy/paste range. Não adicionar mais chrome no toolbar principal.
