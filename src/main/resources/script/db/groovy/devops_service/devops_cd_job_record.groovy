package script.db.groovy.devops_service

databaseChangeLog(logicalFilePath: 'dba/devops_cd_job_record.groovy') {
    changeSet(author: 'wanghao', id: '2020-07-02-create-table') {
        createTable(tableName: "devops_cd_job_record", remarks: '任务记录') {
            column(name: 'id', type: 'BIGINT UNSIGNED', remarks: '主键，ID', autoIncrement: true) {
                constraints(primaryKey: true)
            }
            column(name: 'stage_record_id', type: 'BIGINT UNSIGNED', remarks: '阶段记录Id')
            column(name: 'job_id', type: 'BIGINT UNSIGNED', remarks: '任务Id')
            column(name: 'name', type: 'VARCHAR(50)', remarks: '任务名称')
            column(name: 'type', type: 'VARCHAR(20)', remarks: '任务类型')
            column(name: 'status', type: 'VARCHAR(50)', remarks: '状态')
            column(name: 'trigger_type', type: 'VARCHAR(255)', remarks: '触发方式', defaultValue: 'refs')
            column(name: 'trigger_value', type: 'VARCHAR(255)', remarks: '触发方式对应的值')

            column(name: 'project_id', type: 'BIGINT UNSIGNED', remarks: '项目ID')
            column(name: 'metadata', type: 'VARCHAR(2000)', remarks: 'job详细信息，定义了job执行内容')

            column(name: 'countersigned', type: 'TINYINT UNSIGNED', remarks: '是否会签 1是会签,0 是或签')
            column(name: 'execution_time', type: 'VARCHAR(255)', remarks: '执行时间')
            column(name: "object_version_number", type: "BIGINT UNSIGNED", defaultValue: "1")
            column(name: "created_by", type: "BIGINT UNSIGNED", defaultValue: "0")
            column(name: "creation_date", type: "DATETIME", defaultValueComputed: "CURRENT_TIMESTAMP")
            column(name: "last_updated_by", type: "BIGINT UNSIGNED", defaultValue: "0")
            column(name: "last_update_date", type: "DATETIME", defaultValueComputed: "CURRENT_TIMESTAMP")
        }
    }

    changeSet(author: 'wanghao', id: '2020-07-02-idx-stage-record-id') {
        createIndex(indexName: "idx_stage_record_id ", tableName: "devops_cd_job_record") {
            column(name: "stage_record_id")
        }
    }
    changeSet(author: 'wanghao', id: '2020-07-06-add-column') {
        addColumn(tableName: 'devops_cd_job_record') {
            column(name: 'sequence', type: 'BIGINT UNSIGNED', remarks: '任务顺序')
        }
    }
    changeSet(author: 'wanghao', id: '2020-07-07-add-column') {
        addColumn(tableName: 'devops_cd_job_record') {
            column(name: "started_date", type: "DATETIME", remarks: 'job开始执行时间')
            column(name: "finished_date", type: "DATETIME", remarks: 'job结束时间')
        }
    }
}