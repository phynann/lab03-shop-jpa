# task 1 - Entities and mappings, validated by Flyway

## 1. Column `sku` renamed to `code` in the SQL only

```
Unable to build Hibernate SessionFactory [persistence unit: default] ;
nested exception is org.hibernate.tool.schema.spi.SchemaManagementException:
Schema validation: missing column [sku] in table [product]
```

## 2. Sequence declared `INCREMENT BY 1`

```
Unable to build Hibernate SessionFactory [persistence unit: default] ;
nested exception is org.hibernate.MappingException:
The increment size of the [product_seq] sequence is set to [50] in the entity mapping but the mapped database sequence increment size is [1]
```