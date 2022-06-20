# OnlineAPIDocumentation

## What is it?
Java-приложение,которое ищет все поля и методы (если они public, private или protected) с аннотацией @JsonView({EntityView.Online.class}) и парсит их на имя, тип и поле isMethod.

## How it works?
#### Параметры запуска приложения, упакованного в JAR:
1) --pathToDir (-p) (путь до папки)
2) --withPackageName (-pn) (парсит тип поля или метода с именем пакета или без)
3) --format (-f) (json или html. Если json, в терминал выводится результат в JSON-формате. Если html, в директории с проектом создаётся файл OnlineAPIDocumentation.html)