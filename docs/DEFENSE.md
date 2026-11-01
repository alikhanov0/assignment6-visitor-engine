# Подготовка к защите Assignment 6

**Yerbol Alikhan — SE-2517**

## Что сказать о проекте

«Я сделал Smart Code Inspection and Compliance Engine на Java 17 с паттерном Visitor. Проект представлен деревом ProjectElement: исходники, тесты, конфигурация, зависимости и директории. Директория содержит другие элементы, включая вложенные директории. Операции вынесены в отдельные Visitor: security, statistics, quality gate, compliance и license audit».

Это модель анализа метаданных. Программа не читает реальные исходники и не ищет уязвимости в интернете: признаки и показатели задаются в DemoProjects. Не называй это полноценным статическим анализатором.

## Как работает double dispatch

1. Клиент вызывает `element.accept(visitor)` через `ProjectElement`.
2. Java выбирает реализацию `accept` по реальному типу элемента, например `SourceCodeFile`.
3. Внутри `SourceCodeFile.accept` переменная `this` имеет тип `SourceCodeFile`. Поэтому компилятор выбирает перегрузку `visit(SourceCodeFile)`.
4. Реальная реализация этого метода определяется типом visitor, например `SecurityAuditVisitor`.

Результат зависит от двух типов: элемента и посетителя. Перегрузка выбирается при компиляции, переопределённый метод — во время выполнения.

Если удалить `accept` только у одного record, он перестанет реализовывать интерфейс и не скомпилируется. Если удалить его из интерфейса и всех элементов, общий обход через `ProjectElement` пропадёт: понадобится другой механизм диспетчеризации.

## Что показать в IDE

1. `ProjectElement.java`: единственный метод `accept`.
2. `ProjectVisitor.java`: пять перегрузок `visit`.
3. `SourceCodeFile.java`: `visitor.visit(this)`.
4. `Directory.java`: неизменяемый список дочерних элементов.
5. `RecursiveProjectVisitor.java`: рекурсивный обход принадлежит Visitor.
6. `QualityGateVisitor.java`: использует те же security/statistics visitors, поэтому правила не дублируются.
7. `LicenseAuditVisitor.java`: новый visitor без изменения элементов.
8. `InspectionTest.java` и `LicenseAuditTest.java`: проверки результатов и границ.

## Запуск на защите

```bash
mvn clean verify
java -jar target/smart-code-inspection-1.0.0.jar
git log --oneline --reverse
```

35 тестов должны пройти. В первом проекте 5 файлов, 530 строк, средняя сложность 11, отношение test/source 0.5, пять критических security findings. Quality gate выдаёт FAILED. Во втором проекте средняя сложность 7, критических findings нет, test/source 1.0 — PASSED.

Отношение тестов считается по количеству файлов, а не тест-кейсов или проценту покрытия. Поэтому 0.5 проходит quality gate, но модуль users всё равно нарушает compliance: тест для него не объявлен. Программная декларация покрытия не доказывает реальное покрытие кода.

## Добавить ещё один Visitor

Например, `TechnicalDebtVisitor extends RecursiveProjectVisitor`:

```java
public final class TechnicalDebtVisitor extends RecursiveProjectVisitor {
    private long minutes;

    @Override
    public void visit(SourceCodeFile file) {
        minutes += Math.max(0L, (long) file.complexity() - 10L) * 15L;
    }

    @Override public void visit(TestFile file) { }
    @Override public void visit(ConfigurationFile file) { }
    @Override public void visit(DependencyFile file) { }

    public long minutes() { return minutes; }
}
```

Добавь импорты model и visitor, создай visitor в клиенте, вызови `project.accept(debt)`, затем напиши тест: сложность 14 даёт 60 минут, сложность 8 даёт 0. Сначала предскажи результат, объясни правило, внеси изменение, запусти приложение и тест.

Никакой существующий элемент или `ProjectVisitor` менять не нужно. `visit(Directory)` наследуется от базового класса.

## Добавить DatabaseMigrationFile

Сначала скажи, что это дорогая сторона Visitor: новая операция добавляется легко, новый тип элемента требует обновления всех посетителей.

Добавь record с `implements ProjectElement` и `accept`. Добавь перегрузку `visit(DatabaseMigrationFile)` в интерфейс посетителя. Компилятор укажет на все пять конкретных visitor, которые её не реализовали. Добавь туда поведение, а в `QualityGateVisitor` — передачу двум внутренним visitor. Обнови анонимный recording visitor в тесте, UML и fixture.

`ProjectElement`, пять старых элементов и цикл обхода `RecursiveProjectVisitor` менять не нужно. Абстрактный `RecursiveProjectVisitor` может не реализовывать новый метод, но конкретные классы обязаны это сделать.

## Добавить security rule

Пример: source complexity больше 20 даёт warning. Добавь условие в `SecurityAuditVisitor.visit(SourceCodeFile)`. Для 20 warning не появляется, для 21 появляется. Новое поле модели для этого не требуется.

QualityGate автоматически увидит новое security finding через делегирование. Warning не провалит gate, пока его правило — ноль критических проблем. Если новый finding будет CRITICAL, gate станет FAIL.

## Обработать вложенные директории

`visit(Directory)` перебирает детей и вызывает `child.accept(this)`. Если ребёнок тоже Directory, снова вызывается тот же метод. Тест проверяет 100 уровней. Для очень большой глубины нужен явный стек, иначе возможен StackOverflowError.

Не добавляй второй обход внутрь `Directory.accept`: иначе каждый visitor начнёт обрабатывать детей дважды.

## Отличие от Strategy и SOLID

Strategy выбирает алгоритм для одного контекста. Visitor применяет операцию с разным поведением к разным типам в одном дереве.

SRP — данные отдельно от операций. OCP — новый visitor не меняет модель. LSP — все элементы работают через ProjectElement. DIP — элементы используют интерфейс ProjectVisitor. Ограничение ISP — посетитель знает все типы и иногда реализует пустые методы. OCP тоже ограничен: добавление нового элемента меняет интерфейс посетителей.
