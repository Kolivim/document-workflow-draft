### Исходный запрос:
SELECT * FROM doc_workflow.documents d WHERE d.status = 'DRAFT' ORDER BY d.create_date;

### План выполнения: индексов нет
Sort  (cost=20.39..20.93 rows=215 width=89) (actual time=0.221..0.240 rows=175 loops=1)
    Sort Key: create_date
    Sort Method: quicksort  Memory: 42kB
    Buffers: shared hit=8
    ->  Seq Scan on documents d  (cost=0.00..12.06 rows=215 width=89) (actual time=0.061..0.141 rows=175 loops=1)
        Filter: ((status)::text = 'DRAFT'::text)
        Rows Removed by Filter: 150
        Buffers: shared hit=8
Planning Time: 0.192 ms
Execution Time: 0.414 ms


### Текущая ситуация: индексов нет
Планировщик построил план запроса:
Последовательное сканирование (Seq Scan) таблицы documents, вычитывает 325 строк (175 подошли + 150 отфильтровано),
при этом фильтр применяется к каждой строке таблицы. Время сканирования составляет 0.141 мс (по последней подходящей 
строке, время нахождения первой подходящей строки 0.061 мс).

Сортировка (Sort) - после фильтрации оставшиеся 175 строк сортируются по create_date,
для чего используется quicksort в памяти (42 кБ), время сортировки составляет 0.240 мс.

Общее время выполнения: 0.414 мс - при тестовом объеме данных выглядит приемлемо, 
но с ростом таблицы производительность упадет (особенно учитывая потребность в обработке одним запросом списка 
id размером более 5000 и то что текущий план не масштабируется).


Реализовал составной индекс:
CREATE INDEX idx_documents_status_create_date ON doc_workflow.documents(status, create_date);


### Обновлённая ситуация: создан составной индекс
Sort  (cost=27.05..27.44 rows=155 width=88) (actual time=0.298..0.307 rows=105 loops=1)
    Sort Key: create_date
    Sort Method: quicksort  Memory: 34kB
    Buffers: shared hit=11
    ->  Bitmap Heap Scan on documents  (cost=9.47..21.41 rows=155 width=88) (actual time=0.114..0.225 rows=105 loops=1)
        Recheck Cond: ((status)::text = 'DRAFT'::text)
        Heap Blocks: exact=8
        Buffers: shared hit=11
        ->  Bitmap Index Scan on idx_documents_status_create_date  (cost=0.00..9.44 rows=155 width=0) (actual time=0.100..0.101 rows=115 loops=1)
            Index Cond: ((status)::text = 'DRAFT'::text)
            Buffers: shared hit=3
Planning Time: 0.125 ms
Execution Time: 0.379 ms


Bitmap Index Scan (на индексе idx_documents_status_create_date) проходит по индексу, 
отбирает 115 строк со статусом 'DRAFT', создавая в памяти карту страниц таблицы, где эти строки находятся, 
время построения карты: 0.101 мс. Прочитано 3 страницы индекса (все из кэша).

Bitmap Heap Scan (таблицы documents) по полученной карте читает все 8 страниц таблицы и из каждой страницы извлекает 
подходящие строки, с перепроверкой условия, время сканирования от первого результата 0.114 мс и до последнего 0.225 мс, 
извлечено 105 строк.

Сортировка (Sort) — полученные 105 строк сортируются по полю create_date, с использованием алгоритма quicksort 
в памяти (34 КБ), время сортировки составляет 0.307 мс.

Общее время выполнения: 0.379 мс — при текущем объеме данных (105 строк результата) выглядит отлично.


На имеющемся небольшом объёме прироста производительности не произошло, в т.ч. возможно по причине того, что таблица 
на данном этапе целиком помещается в кэш 

