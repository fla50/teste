# Cinematic Containers (Minecraft 26.3, Fabric)

Addon do Cinematic Interactions. Baú, baú com armadilha, barril, baú do Ender e todos
os shulkers abrem em primeira pessoa: a câmera se inclina sobre o container e os itens
aparecem flutuando em grade em cima dele. Clique num item para pegar; clique num espaço
vazio com um item na mão para guardar.

Precisa de: Fabric Loader 0.19.5+, Fabric API 0.163.0+26.3, Java 25 e o
**Cinematic Interactions 0.18.30** (o mesmo .jar que você enviou).

## Como gerar o .jar (sem programar)

1. Crie uma conta em github.com e um repositório novo (pode ser privado).
2. Na página do repositório, clique em **Add file > Upload files** e arraste TODO o
   conteúdo desta pasta (inclusive a pasta `.github` e a pasta `libs`).
3. Confira que dentro da pasta `libs` está o arquivo
   `Cinematic_Interaction-Fabric-26_3-0_18_30.jar` (já vem junto; se não subiu, envie ele
   com esse nome exato).
4. Clique em **Commit changes**.
5. Abra a aba **Actions**. Vai aparecer um trabalho chamado "build" rodando.
   Quando ficar com ✔ verde, clique nele e baixe **cinematic-containers** em "Artifacts".
6. Dentro do zip baixado está o `cinematic-containers-0.1.0.jar`. Ponha na pasta `mods`,
   junto com o Cinematic Interactions e a Fabric API.
7. Se ficar ❌ vermelho: clique no trabalho, abra o passo "Compilar", copie as linhas com
   `error:` e cole para mim no chat.
