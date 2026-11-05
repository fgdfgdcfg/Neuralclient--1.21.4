# Рабочий конспект: Neural client (MCP / 1.21.4 yarn build.8, Fabric, Java 21)

## Проект / среда
- Рабочая папка: `C:\Users\Minecraft love\Downloads\Neuralclient -1.21.4`
- Исходники: `src\main\java\fun\neural\**` + порт Nova под `src\main\java\client\**`
- Сборка: `.\gradlew.bat compileJava --console=plain -q` (тихий успех = нет вывода)
- Пользователь запускает дев-сборку из IntelliJ IDEA; логи в `run\logs\latest.log`; общение на русском; результат сообщает словами.

## Текущий статус (31.08)
Экспериментальный клик-гуи (пакет `fun.neural.client.ui.aurora`, стиль «Neural») **создан → допилен по фидбеку → удалён по просьбе пользователя** («говно»):
- Удалён весь пакет `fun.neural.client.ui.aurora` (AuroraScreen/State/Layout/Renderer/InputHandler).
- `KeyBoardUtils` RShift → снова всегда `new MenuPanel()`.
- `Interface.guiStyle` → `new ModeSetting("GUI", "Обычный", "Обычный")`.
- `compileJava` — SUCCESS. Порт Nova не тронут, старый кликгуи (MenuPanel/clickgui) работает как раньше.

## Проект в рабочем состоянии
- Новых незавершённых задач нет; изменения сводятся к откату эксперимента.

## Уроки/история (чтобы не возвращаться)
- Клик-гуи на базе `AudioState`-подхода (единая панель, рельс категорий, поиск): юзер оценил негативно («буква N улетает», «бинокль поиска улетает» — кривизна центровки мелких глифов MSDF; «белая фигня резко появляется/исчезает» на ховере строк). Правки центровки по метрикам шрифта, иконка лупы `icon1(18)` на фикс. позиции, theme-тинированный hover — результата не спасли; гуи удалён. Повторно строить в этом стиле не стоит, либо сразу спрашивать эскиз у пользователя.
- Порт Nova (`src\main\java\client\gui\screen\ClickGuiScreen.java`, `client\render\ShapeShader.java`) после правок компилировался, но ПАНЕЛИ НЕ РИСОВАЛИСЬ (скрин: чёрные участки = мир ночью, сквозь них видно интерфейс); тема на рендере светлая (bg=ffffffff), GUI-ортопроекция 640x512, позиции/данные валидны (`contentAreaRows=17`, категории: COMBAT=18 MOVEMENT=17 PLAYER=21 RENDER=36 VISUAL=22 CLIENT=5) — несоответствие именно в выводе фигур. Дебаг закрыт (пользователь просил откат); `Nova` в guiStyle не возвращать до фикса.
- Раз откат: убран `[NOVA-DBG2]` дамп, маркеры (red fill / TEXT-MARK / magenta circle), поле `debugPrinted` и лишние импорты `RenderSystem`/`BufferRenderer` из ClickGuiScreen.
- Ранее починен runtime-краш: `ConfigSync.java` сортировал неизменяемый `List.of()` (нужна mutable-копия).
- Урок по API: `RenderUtils` = `@UtilityClass` (методы «инстансные» в коде становятся статическими); правильный `ModuleClass` = `fun.neural.api.storages.implement.helpertstorages.enumvar.ModuleClass` (`INSTANCE.getObject()`, статич. `interfaceModule.guiOpacity.get()`); `Module.ModuleCategory` (enum, `getIcons()` b/c/d/h/e); `Fonts.getFont("suisse"|"icon"|"icon1", int)`; `ScissorUtils.push/setFromComponentCoordinates/unset/pop`.

## Полезные пути
- `src\main\java\fun\neural\client\ui\MenuPanel.java` — эталонный Screen (анимации, звуки, in/out).
- `src\main\java\fun\neural\client\ui\clickgui\*` — эталонный кликгуи (ClickGuiRenderer/SettingRenderer/InputHandler/State/Layout).
- `fun.neural.api.utils.render.RenderUtils` — drawRoundedRect (4-угла), drawGradientRect (верт/гор/4-угла), drawGradient6Rect, drawRoundCircle, drawShadow(radius/softness), drawRoundedRectOutline, drawBlur, drawLiquidGlass.
- `fun.neural.api.utils.color.ColorUtils` — applyAlpha, darken, getThemeColor([index]), rgba, gradient, overCol, multAlpha/multDark/setAlphaColor.
- `fun.neural.api.utils.render.fonts.msdf.Font` — draw/drawCenteredString/drawRight/drawGradientStringHorizontal/getWidth/getHeight.

## Следующие шаги (по желанию пользователя)
- При желании: починить рендер порта Nova и вернуть «Nova» в guiStyle только после фикса бага фигур.
- Или вернуться к любой прежней задаче из истории (TLauncher-деплой, конфиг-краш — уже закрыто).