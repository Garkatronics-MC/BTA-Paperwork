{
  description = "BTA-Paperwork (Better Than Adventure mod) development environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs = { self, nixpkgs, ... }:
    let
      system = "x86_64-linux";
      pkgs = nixpkgs.legacyPackages.${system};

      # Fuentes para java.awt (Minecraft las usa al generar texturas)
      fontsConf = pkgs.makeFontsConf { fontDirectories = [ pkgs.dejavu_fonts ]; };

      commonPackages = with pkgs; [
        jdk17      # Toolchain del mod (sourceCompatibility = 17)
        jdk21      # Gradle / build tooling
        gradle     # Opcional: el proyecto usa ./gradlew
        git
        curl
      ];

      commonEnv = ''
        export JAVA_HOME=${pkgs.jdk21}
        export PATH=${pkgs.jdk21}/bin:${pkgs.jdk17}/bin:$PATH
        export FONTCONFIG_FILE=${fontsConf}
      '';

      # Librerias comunes (fuentes + Wayland)
      baseLibs = with pkgs; [ fontconfig.lib freetype wayland libxkbcommon ];

      # Librerias X11 solo para el shell headless (Xvfb)
      x11Libs = with pkgs; [
        libglvnd mesa libX11 libXrandr libXinerama libXxf86vm libXcursor libXi
      ];
    in {
      devShells.${system} = {

        # ---------------------------------------------------------------
        # default: ventana real en Wayland con la GPU
        #   nix develop --command ./gradlew runClient
        # ---------------------------------------------------------------
        default = pkgs.mkShell {
          name = "paperwork-dev";
          nativeBuildInputs = commonPackages;

          shellHook = commonEnv + ''
            export LD_LIBRARY_PATH=/run/opengl-driver/lib:${pkgs.lib.makeLibraryPath baseLibs}:$LD_LIBRARY_PATH

            # Solo para el juego (runClient): GLFW con Wayland
            export BTA_GLFW_LIB=${pkgs.glfw3-minecraft}/lib/libglfw.so
          '';
        };

        # ---------------------------------------------------------------
        # headless: Xvfb + llvmpipe (CPU). Para CI o SSH sin pantalla
        #   nix develop .#headless --command ./gradlew runClient
        # ---------------------------------------------------------------
        headless = pkgs.mkShell {
          name = "paperwork-dev-headless";
          nativeBuildInputs = commonPackages ++ (with pkgs; [
            xorg-server   # Xvfb
            mesa-demos    # glxinfo
            xdpyinfo
            xwd
            imagemagick   # capturas: xwd -root -display :98 | magick xwd:- shot.png
          ]);

          shellHook = commonEnv + ''
            export LD_LIBRARY_PATH=${pkgs.lib.makeLibraryPath (baseLibs ++ x11Libs)}:$LD_LIBRARY_PATH

            # Renderizado por software
            export LIBGL_DRIVERS_PATH=${pkgs.mesa}/lib/dri
            export LIBGL_ALWAYS_SOFTWARE=1
            export GALLIUM_DRIVER=llvmpipe
            export MESA_LOADER_DRIVER_OVERRIDE=swrast
            export __GLX_VENDOR_LIBRARY_NAME=mesa

            # Xvfb con GLX habilitado (sin esto: "Failed to find a suitable GLXFBConfig")
            export DISPLAY=:98
            pkill -x Xvfb 2>/dev/null || true
            rm -f /tmp/.X98-lock /tmp/.X11-unix/X98
            Xvfb :98 -screen 0 1280x720x24 +extension GLX +extension RANDR +render +iglx -noreset >/dev/null 2>&1 &
            sleep 2
          '';
        };
      };
    };
}
