# KidTube

Feed de vídeos **seguro para crianças**, para Android TV / Fire TV e celular Android.
Os pais escolhem exatamente o que entra (links do YouTube ou vídeos salvos no aparelho) e
organizam tudo em **canais numerados**, como uma TV a cabo. A criança não vê busca do
YouTube, recomendações nem comentários: só o que foi adicionado.

> Antes se chamava **MeuTube** / KidsTV. O nome visível passou a ser **KidTube**; o
> identificador do pacote continua `com.kidstv.app` para que a atualização não apague
> os dados de quem já tem o app instalado.

## O que o app faz

**Na TV (Android TV / Fire TV, controle remoto)**
- Feed de vídeos, busca local, histórico de assistidos.
- Canais/playlists numerados: `Canal +` / `Canal -`, digitar o número do canal e **Guia**.
- Trava parental com senha (PIN) para sair do modo criança e mexer nas configurações.
- **Painel dos Pais pelo celular**: a TV mostra um QR Code; no navegador do celular dá para
  colar URLs do YouTube, enviar vídeos do celular e gerenciar canais. A TV sobe um
  servidor local (NanoHTTPD) e se anuncia na rede via NSD (`_kidstv._tcp`).
- Controle do espaço de armazenamento da TV para os vídeos enviados.

**No celular**
- Mesmo catálogo, canais e histórico, com trava de tela.
- **Conectar TV**: acha a TV na rede (ou pelo endereço, ex. `http://192.168.1.50:8787`) e
  manda vídeos para tocar lá.
- **Espelhar na TV**: atalho para Miracast/Smart View e modo "Só lista no celular",
  que mostra o vídeo no display externo.

## Tecnologia

Kotlin + Jetpack Compose (Material 3), Room (banco local), Media3/ExoPlayer para vídeos
locais, player do YouTube embutido para links, Coil, ZXing (QR Code), NanoHTTPD (painel web).
`minSdk 26`, `targetSdk 35`. Interface em português.

```
app/src/main/java/com/kidstv/app/
├── data/        Room (vídeos, playlists, histórico), repositório, PIN dos pais, armazenamento
├── player/      player do YouTube, player local, display externo (espelhamento)
├── server/      servidor local da TV, cliente do celular, HTML do Painel dos Pais
├── ui/tv/       interface de TV (D-pad)
├── ui/mobile/   interface de celular
└── ui/components, ui/theme, util/
```

## Compilar e instalar

Requer JDK 17 e Android SDK (o caminho vai em `local.properties`, que não é versionado).

```sh
./gradlew assembleDebug              # APK em app/build/outputs/apk/debug/
./gradlew testDebugUnitTest          # testes unitários
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Na Fire TV, o app aparece em **Seus apps** com o nome KidTube.
