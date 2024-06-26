# Отчет по hw3

## Переход на несколько нод

Выбор ноды для партицирования происходил с помощью алгоритма `Consistent hashing`

После нагрузочного тестирования put стало яснее, что алгоритм равномерно распределяет ключи между нодами.

[картинка](https://disk.yandex.ru/i/9wJKJ78oDO5VLw)

### PUT

Перед профилированием нод база была пустой.

Нагрузка при точке разладки для 3 нод

1 нода (при нагрузке 52к рпс)

Latency Distribution (HdrHistogram - Recorded Latency)

| percents | latency |
----------| --------
| 50.000% |    1.27ms |
| 75.000% |    1.77ms |
| 90.000% |    2.22ms |
| 99.000% |    8.42ms |
| 99.900% |   12.52ms |
| 99.990% |   13.97ms |
| 99.999% |   14.75ms |
| 100.000% |   15.14ms |

3 ноды (при нагрузке 52к рпс)

Latency Distribution (HdrHistogram - Recorded Latency)

| percents | latency |
----------| --------
| 50.000% |    1.74ms |
| 75.000% |    2.48ms |
| 90.000% |    3.26ms |
| 99.000% |    4.60ms |
| 99.900% |    5.57ms |
| 99.990% |    6.70ms |
| 99.999% |    7.26ms |
| 100.000% |    7.61ms |

И [графики](https://disk.yandex.ru/i/IPg_ZFYfbDpz_g)

На данной нагрузке видно, что latency у 1 ноды меньше на 95%
При этом самые долгие запросы обрабатываются дольше. 
Мое предположение, что так происходит, что из-за слишком маленькой нагрузке,
потоки чаще засыпают и просываются и поэтому так происходит.

### GET

Перед профилированием get база была заполнена 1,5 миллиона ключей (~100мб).

Для GET точка точка разладки при 51 рпс.

1 нода (при нагрузке 51к рпс)

Latency Distribution (HdrHistogram - Recorded Latency)

| percents | latency |
----------| --------
| 50.000%|    1.28ms |
| 75.000% |   1.77ms |
| 90.000%|    2.21ms |
| 99.000%|    2.79ms |
| 99.900%|    3.16ms |
| 99.990%|    3.57ms |
| 99.999%|    3.94ms |
| 100.000%|    4.28ms |

3 ноды (при нагрузке 51к рпс)

Latency Distribution (HdrHistogram - Recorded Latency)

| percents | latency |
----------| --------
| 50.000% |    1.58ms |
| 75.000% |    2.05ms |
| 90.000% |    2.72ms |
| 99.000% |    3.85ms |
| 99.900% |    6.06ms |
| 99.990% |    7.24ms |
| 99.999% |    7.96ms |
| 100.000% |    8.27ms |

И [графики](https://disk.yandex.ru/i/1jK8XfCYe_APKg)

При той же нагрузке у нас в три раза меньше latency на 2 девятках.

## Профилирование (PUT)

Перед профилированием нод база была пустой.

### cpu

А что вообще изменилось. В cpu появилось плато, которое занимает 23 процента - пересылка ответа (выполнение локального ответа занимает около 1 процента).

[Страничка для 1 ноды](https://disk.yandex.ru/d/lOXWN9RZ1_F0jg)

[Страничка для 3 нод](https://disk.yandex.ru/d/vUmq8eU-VVrV3w)

### alloc

В аллокациях появилось плато которое занимает 75 процентов - все на чтение ответа из пересылки (_здесь должен быть эмодзи клоуна_)

[Страничка для 1 ноды](https://disk.yandex.ru/d/UPG1QMrugqvdzA)

[Страничка для 3 нод](https://disk.yandex.ru/d/UqV-V2bnm_S5jA)

### lock

Если лок на `HttpSession` раньше занимал 7 процентов, то с тремя нодами он занимает 30 процентов.

[Страница для 1 ноды](https://disk.yandex.ru/d/fMXh-jQOWk1taA)

[Страница для 3 нод](https://disk.yandex.ru/d/FjroZlzZ0hWsbg)

## Профилирование (GET)

Перед профилированием get база была заполнена 1,5 миллиона ключей (~100мб).

### cpu 

В cpu появилось плато 21 процентов на пересылку.

[Страница для 1 ноды](https://disk.yandex.ru/d/xQV_ftJCG6cLpw)

[Страница для 3 нод](https://disk.yandex.ru/d/9SUfhMG5AiW-iQ)

### alloc

Появилось alloc появился расход на пересылку в 27 процентов (До этого 97 процентов всех памяти занимали `MemorySegmentы` в бинарном поиске)

[Страница для 1 ноды](https://disk.yandex.ru/d/9XypfNFB_xselw)

[Страница для 3 нод](https://disk.yandex.ru/d/9NpC3Jy1KiwYaA)

### lock

`HttpSession` занимал по локам меньше процента, а теперь занимает 7 процентов.

[Страница для 1 ноды](https://disk.yandex.ru/d/NDfXZBRuohjnAg)

[Страница для 3 нод](https://disk.yandex.ru/d/ZlDAmrCJupOZyQ)

## Сравнение gc

### PUT

На put сравнивать мне не так интересно было, так как у меня явно большая часть памяти утекает на get во время бинарного поиска.

Тем не менее [графики](https://disk.yandex.ru/i/rjqfquYyRgiGOg)

Видно, что `SerialGC` и `ParallelGC` выдали большее latency, чем `G1GC` и `ZGC` на той же нагрузке (52к рпс)

Было предположение, что `SerialGC` может справить лучше из-за маленькой кучи, так как отсутствует переключение контекстов.
Также хочу заметить, что `ZGC` справился немного лучше (по latency) `G1GC` примерно на всех участках. `ShenandohGC` я не смог завести, но предположу, что он не справится хуже (latency было бы больше, если бы вообще заработала vm),
тем более что его не рекомендуют запускать на маленьких кучах.

### GET

А вот get оказался интересней

[графики](https://disk.yandex.ru/i/UWcKus3KTZkNmw)

Наименьшее latency на 4 девятках оказалось у `SerialGC`. Что же мне было еще интересней увидеть - `ZGC` смог выдать почти такую же latency почти на всем промежутке до 4 девяток,
а суммарно выдал лучше результаты.

Я сделал вывод, что стоит присматриваться к `ZGC` в любом случае, а также не забывать, что в некоторых случаях `ParallelGC` и `SequentialGC` имеют смысл. 
