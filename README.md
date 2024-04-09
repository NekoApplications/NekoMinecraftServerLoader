# NekoMinecraftServerLoader

### NekoMinecraftServerLoader is a scriptable Minecraft server launcher

## Supported Minecraft Server Types

- √ Vanilla

## Supported ModLoaders

- √ [Fabric](https://fabricmc.net/)
- × [Forge](https://forums.minecraftforge.net/) (Working in progress)
- × [Neoforge](https://neoforged.net/) (Working in progress)
- × [Quilt](https://quiltmc.org/) (Working in progress)

## Supported Mod Repositories

- √ [Modrinth](https://modrinth.com/)
- × [Curseforge]() (Working in progress)

## CLI Arguments

```bash
java -jar <NekoMinecraftServerLoader_jarfile> <action>
```

Argument `action` can be `runServer` or `buildServerZip`.

| Action           | Argument 1                 | Arg default     |
|------------------|----------------------------|-----------------|
| `runServer`      | No args                    |                 |
| `buildServerZip` | `-o [Server zip FileName]` | `-o server.zip` |

If no arguments are provided, scriptName will be default to `build.server.kts`, action will be default to `runServer`

## Config Script Example

```kotlin
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

minecraft {
    version("1.20.1") //specify server version
    modLoader {
        fabric // adds fabric modloader
        version("0.15.0") //specify modloader version to 0.15.0
    }
    modRepository {
        modrinth // adds modrinth to mod repositories

    }
    launch {
        jvmArgs("-Xmx16G", "-Xms1G") // add jvm args
        args("--nogui") // add server args
        before { //procedures to execute before server starts
            execute("bye")
        }
        after { // procedures to execute after server stopped
            execute("printWorkingDir")
        }
    }
    mods {
        modid("fabric-api") // add mod fabric-api
        modid("lithium") version "mc1.20.1-0.11.2" // add mod lithium and set its version to "mc1.20.1-0.11.2"
        modid("no-chat-reports") { // add mod no-chat-reports
            rename("ncp.jar") // when this mod are going to be installed, rename this mod to "ncp.jar"
        }
    }
}
procedure("bye") {// defines a procedure named bye
    println("Bye!")
}

procedure("printWorkingDir") {// defines a procedure named printWorkingDir
    println("Root Working dir:" + workingDir.toAbsolutePath()) //the workingDir field is pre-defined by ScriptDef
}
```
