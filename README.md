# ReadMyItem

**Source Code / Issues:** [https://github.com/mmlo/ReadMyItem](https://github.com/mmlo/ReadMyItem)

**Author:** mml  
**Minecraft:** Java Edition **1.21.11** · **Loader:** Fabric · **Side:** Client only  
**License:** GPL-3.0-or-later

---

## English

Hovering an item in any inventory reads its name and description aloud in English or Brazilian Portuguese. Built for accessibility and kids who are still learning to read: the voices are bundled in the jar, with no extra downloads and no programs launched on the PC.

### What it does

When a player opens a chest, the player inventory, a furnace, Creative, or any other container and **hovers the mouse over an item**, the game **speaks the name** in English or Portuguese (based on your Minecraft language). If the cursor stays a little longer, it also speaks the description (enchantments, durability, and so on).

Speech uses **Piper TTS inside the same Minecraft process**. The mod does **not** run extra programs on the computer, does not need a microphone, and does **not** use Whisper.

The vanilla tooltip already shows the name. Speech is the extra help.

### Requirements

- Minecraft Java Edition **1.21.11**
- [Fabric Loader](https://fabricmc.net/use/installer/)
- [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.11
- This mod (`ReadMyItem-….jar`) — **English and Portuguese voices are already in the jar**
- Recommended: [Mod Menu](https://modrinth.com/mod/modmenu) and [Cloth Config](https://modrinth.com/mod/cloth-config)

Speech works on **Windows 64-bit**, **Linux 64-bit**, and **macOS Intel and Apple Silicon**.

### How to install

1. Install Fabric for Minecraft 1.21.11.
2. Put Fabric API and `ReadMyItem-….jar` in the `mods` folder.
3. Optional but helpful: also add Mod Menu and Cloth Config.
4. Start the game, open an inventory, hover an item, and wait about half a second. **You do not need to download a voice.**

On first launch, the mod copies the bundled Piper files from the jar into `config/readmyitem/voices/`. Official voices: **Lessac** (English), **Faber**, and **Edresson** (Portuguese).

### How to use

1. Open any inventory (player, chest, furnace, Creative, villager, shulker…).
2. Hover the item and **wait about 0.5 seconds**.
3. The game speaks the name (and the count, if there is more than one).
4. If the mouse stays about one more second, it speaks the description.
5. The mod auto-detects your Minecraft language. If you play in English, it reads in English. If you play in Portuguese, it reads in Portuguese. You can change the voice model in the Mod Menu settings.

Keys (all unbound by default; you can set them in Controls):

- toggle the mod
- repeat the last item
- stop speech

### What it does not do

- Does not run `piper.exe` or any other OS program
- Does not need a microphone
- Does not use Whisper / faster-whisper / voice commands
- Does not read blocks in the world, chat, books, or signs
- Does not give a PvP advantage: it only reads what is already on screen

### If something does not work

If your problem is not listed here, please [open an issue on GitHub](https://github.com/mmlo/ReadMyItem/issues).

| Problem | What to do |
| --- | --- |
| Name shows, but there is no speech | Wait for the first-launch voice extract. Check the log for `ReadMyItem`. |
| Nothing happens | Make sure the mod is enabled and game volume **Voice** / **Master** is not muted. |
| Native / Piper error in the log | Your OS may be unsupported. The game still runs; only speech turns off. |

### License

**GPL-3.0-or-later**, because in-process Piper (piper-jni + Piper natives) is GPL in the combined jar.  
Voice models: MIT, Rhasspy / Lessac, Faber, and Edresson authors.

---

## Português (Brasil)

Ao passar o mouse sobre um item em qualquer inventário, o jogo lê o nome e a descrição em inglês ou português do Brasil. Feito para acessibilidade e crianças que ainda estão aprendendo a ler: as vozes já vêm no jar, sem download extra e sem abrir programas no PC.

### O que faz

Quando o jogador abre um baú, o inventário, uma fornalha, o criativo ou qualquer outro contêiner e **passa o mouse sobre um item**, o jogo **fala o nome** no idioma do jogo. Se o cursor ficar um pouco mais, fala também a descrição (encantamentos, durabilidade etc.).

A fala usa **Piper TTS no mesmo processo do Minecraft**. O mod **não executa programas no computador**, não precisa de microfone e **não usa Whisper**.

O tooltip vanilla já mostra o nome. A fala é o extra.

### O que você precisa

- Minecraft Java **1.21.11**
- [Fabric Loader](https://fabricmc.net/use/installer/)
- [Fabric API](https://modrinth.com/mod/fabric-api) para 1.21.11
- Este mod (`ReadMyItem-….jar`) — **as vozes em Inglês e PT-BR já vão no jar**
- Recomendado: [Mod Menu](https://modrinth.com/mod/modmenu) e [Cloth Config](https://modrinth.com/mod/cloth-config)

Sistemas com fala: **Windows 64 bits**, **Linux 64 bits**, **macOS Intel e Apple Silicon**.

### Como instalar

1. Instale o Fabric para Minecraft 1.21.11.
2. Coloque o Fabric API e o `ReadMyItem-….jar` na pasta `mods`.
3. Opcional, mas ajuda: coloque também Mod Menu e Cloth Config.
4. Abra o jogo, abra um inventário, passe o mouse num item e espere cerca de meio segundo. **Não precisa baixar voz.**

Na primeira vez o mod copia os arquivos Piper do próprio jar para a pasta `config/readmyitem/voices/`. Vozes oficiais embutidas: **Lessac** (Inglês), **Faber** e **Edresson** (Português).

### Como usar

1. Abra qualquer inventário (jogador, baú, fornalha, criativo, villager, shulker…).
2. Passe o mouse sobre o item e **espere cerca de 0,5 s**.
3. O jogo fala o nome (e a quantidade, se for mais de um).
4. Se o mouse ficar mais cerca de um segundo, fala a descrição.
5. O mod detecta o idioma do Minecraft automaticamente e define a voz padrão. Você pode trocar o modelo de voz nas configurações do Mod Menu.

Teclas (todas vêm **sem atalho**; você pode ligar em Controles):

- ligar/desligar o mod
- repetir o último item
- parar a fala

### O que o mod não faz

- Não executa `piper.exe` nem nenhum programa do Windows/Linux/macOS
- Não precisa de microfone
- Não usa Whisper / faster-whisper / comando de voz
- Não lê blocos no mundo, chat, livros ou placas
- Não dá vantagem em PvP: só lê o que já está na tela

### Se algo não funcionar

Se o seu problema não estiver listado aqui, por favor [abra uma issue no GitHub](https://github.com/mmlo/ReadMyItem/issues).

| Problema | O que fazer |
| --- | --- |
| Nome aparece, mas não fala | Espere a extração da voz na primeira abertura. Veja o log `ReadMyItem`. |
| Nada acontece | Confira se o mod está ligado e se o volume **Voz** / **Geral** do jogo não está no mudo. |
| Erro de nativo / Piper no log | Seu sistema pode não ser um dos suportados. O jogo continua; só a fala desliga. |

### Licença

**GPL-3.0-or-later**, porque o Piper in-process (piper-jni + nativos do Piper) é GPL no JAR combinado.  
Modelos de voz: MIT, Rhasspy / autores Lessac, Faber e Edresson.
