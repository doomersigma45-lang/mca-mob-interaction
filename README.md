# MCA Everywhere (Forge 1.20.1)

Leva o menu de interação do Minecraft Comes Alive para **qualquer mob**, inclusive de outros mods.

## Como usar no jogo
- **Shift + botão direito** num mob abre o menu (pode mudar em `config/mca_everywhere-common.toml`).
- Botões: **Conversar**, **Contar piada**, **Presente** (usa o item da mão), **Abraçar**, **Siga-me**, **Fique aqui**, **Mover livremente**.
- Cada mob tem uma amizade (0–10 corações) **por jogador**, salva no NBT do mob.
- Presentes: usa os arquivos `data/*/gifts/*.json` do MCA (campo `items`) quando o MCA está instalado; sem ele, usa uma tabela interna (comida, flores, esmeralda, diamante...).
- Amigos (amizade >= 10 por padrão) não atacam você, nem mobs hostis. Bater no mob derruba a amizade.
- Aldeões e mobs do próprio MCA continuam com o menu original dele (são ignorados por este mod).

## Compilar (precisa de JDK 17)
```
./gradlew build        # Windows: gradlew.bat build
```
O jar sai em `build/libs/mca_everywhere-1.0.0.jar`. Coloque na pasta `mods` junto com o MCA (Forge 47.x, 1.20.1).
Sem JDK instalado? Suba esta pasta para um repositório no GitHub: o workflow em `.github/workflows/build.yml`
compila sozinho e deixa o jar em Actions > build > Artifacts.

## Observações
- Mobs com "Brain" (aldeão vanilla, piglin, allay, rã, etc.) seguem de forma "melhor esforço"; o Brain pode brigar com o comando de seguir.
- "Fique aqui" liga o `NoAI` do mob (e restaura o valor original ao soltar). Se você remover o mod com mobs parados, eles ficam sem IA; solte-os antes.
- Configurações: gatilho, amizade mínima para seguir, amizade para parar de atacar, distância de teleporte e lista de mobs bloqueados.
