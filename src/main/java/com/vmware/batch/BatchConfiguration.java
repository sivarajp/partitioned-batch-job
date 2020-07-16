package com.vmware.batch;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.cloud.task.configuration.DefaultTaskConfigurer;
import org.springframework.cloud.task.configuration.TaskConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import com.vmware.batch.model.Person;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

  @Autowired
  public JobBuilderFactory jobBuilderFactory;

  @Autowired
  public StepBuilderFactory stepBuilderFactory;


  @Bean
  public FlatFileItemReader<Person> reader() {
    return new FlatFileItemReaderBuilder<Person>()
      .name("personItemReader")
      .resource(new ClassPathResource("sample-data.csv"))
      .delimited()
      .names(new String[]{"firstName", "lastName"})
      .fieldSetMapper(new BeanWrapperFieldSetMapper<Person>() {{
        setTargetType(Person.class);
      }})
      .build();
  }

  @Bean
  public PersonItemProcessor processor() {
    return new PersonItemProcessor();
  }

  
  @Bean(name = "appDatasource")
  @ConfigurationProperties(prefix = "app.datasource")
  public DataSource sourceDataSource() {
      return DataSourceBuilder.create().build();
  }

  @Bean
  @Primary
  @ConfigurationProperties(prefix = "spring.datasource")
  public DataSource springDataSource() {
      return DataSourceBuilder.create().build();
  }
  
  
  @Bean
  public JdbcTemplate jdbcTemplate(@Qualifier("appDatasource") DataSource dataSource)
  {
      return new JdbcTemplate(dataSource);
  }

  @Bean
  public TaskConfigurer taskConfigurer() {
      return new DefaultTaskConfigurer(springDataSource());
  }
  

  @Bean
  public JdbcBatchItemWriter<Person> writer(@Qualifier("appDatasource") DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<Person>()
      .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
      .sql("INSERT INTO people (first_name, last_name) VALUES (:firstName, :lastName)")
      .dataSource(dataSource)
      .build();
  }
  
  @Bean
  public Job importUserJob(JobCompletionNotificationListener listener, Step step1) {
    return jobBuilderFactory.get("importUserJob")
      .incrementer(new RunIdIncrementer())
      .listener(listener)
      .flow(step1)
      .end()
      .build();
  }

  @Bean
  public Step step1(JdbcBatchItemWriter<Person> writer) {
    return stepBuilderFactory.get("step1")
      .<Person, Person> chunk(10)
      .reader(reader())
      .processor(processor())
      .writer(writer)
      .build();
  }
}