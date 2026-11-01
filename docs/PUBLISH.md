# Публикация проекта с сохранением коммитов

В проекте уже есть локальная Git-история. Публикация в GitHub требует входа в твой аккаунт. Из этой сессии проект в аккаунт не отправлен.

## Если распаковал архив

Открой в VS Code папку `assignment6-visitor-engine`, затем Terminal → New Terminal. Внутри уже есть `.git`, поэтому `git init` повторно не нужен.

```bash
git log --oneline --reverse
git status
```

Если история видна, продолжай с созданием репозитория. Системная папка `.git` может быть скрыта в Проводнике Windows — это нормально. Не удаляй её.

## Если используешь Git bundle

В архив включена резервная копия всей истории `assignment6-history.bundle`. Из папки, где лежит bundle, можно восстановить отдельный репозиторий:

```bash
git clone assignment6-history.bundle assignment6-restored
cd assignment6-restored
git remote remove origin
git log --oneline --reverse
```

Не выполняй этот путь поверх уже распакованной папки с проектом: выбери один способ.

## Создать публичный GitHub репозиторий

1. Открой https://github.com/new под своим аккаунтом.
2. Назови репозиторий `assignment6-visitor-engine`.
3. Выбери **Public** — это требование задания.
4. Не добавляй на сайте README, `.gitignore` и license: репозиторий должен быть пустым, чтобы история не конфликтовала.
5. Скопируй HTTPS URL созданного репозитория.

В терминале проекта выполни команды, заменив `YOUR_USERNAME` на свой GitHub username:

```bash
git remote add origin https://github.com/YOUR_USERNAME/assignment6-visitor-engine.git
git push -u origin main
```

Git может открыть браузер для входа. Обычный пароль GitHub не используется для HTTPS push; войди через Git Credential Manager либо используй настроенный SSH remote.

Если `origin` уже существует, проверь его командой `git remote -v`. Для замены на созданный пустой репозиторий используй `git remote set-url origin URL`, затем обычный push. Force push здесь не нужен.

## Что сдавать

Проверь на GitHub, что README отображается, история содержит не меньше восьми коммитов, а отдельный коммит `feat: add independent license audit visitor without changing project elements` виден в списке. Во вкладке Actions должен пройти Java 17 workflow.

В Moodle отправь URL публичного репозитория и README в соответствии с полем загрузки задания. Не отправляй только ZIP вместо URL: преподавателю требуется GitHub и история коммитов.
