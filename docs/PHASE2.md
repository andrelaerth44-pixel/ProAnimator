# Phase 2 — Status Atual

## Perform Mode (NOVO)

Agora o Perform mode captura movimento real:

1. Mude para o modo **Perform**
2. Aperte **Rec**
3. Arraste o dedo/stylus pela tela enquanto a timeline toca
4. Os movimentos são gravados como keyframes de **Position X** e **Position Y** (easing Linear)
5. Aperte **Stop** ou deixe chegar ao final

Quando você der Play depois, o conteúdo se move seguindo o caminho que você performou.

## Transforms visuais

- Opacity, Scale, Rotation e Position já são aplicados no canvas via `withTransform`
- Você vê o resultado em tempo real ao dar Play

## Onion Skin clássico
- Anterior = tons de vermelho/laranja
- Próximo = tons de verde/ciano

## Resumo do que a Phase 2 já entrega

| Feature                    | Status      |
|---------------------------|-------------|
| Timeline + Playhead       | ✅          |
| Modos Compose/Keyframe/Perform | ✅     |
| Flipbook + frames         | ✅          |
| Onion Skin colorido       | ✅          |
| Keyframes (Op, Pos, Scale, Rot) | ✅   |
| Interpolação (Linear + Eases) | ✅     |
| Perform captura movimento | ✅          |
| Transforms visuais        | ✅ Básico   |

## Próximos refinamentos

- Keyframes visuais na timeline (marcadores)
- Melhor pivot de Scale/Rotation
- Perform também para Scale/Rotation/Opacity
- Ligação mais forte Content ↔ Track

---

**Phase 2 está muito avançada.**
