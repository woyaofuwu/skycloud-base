/*
 * The MIT License (MIT)
 * Copyright © 2019 <sky>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.skycloud.base.codegen.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import com.skycloud.base.codegen.common.CommonConstants;
import com.skycloud.base.codegen.common.enums.DALTypeEnum;
import com.skycloud.base.codegen.exception.CheckedException;
import com.skycloud.base.codegen.model.po.ColumnEntity;
import com.skycloud.base.codegen.model.po.Codegen;
import com.skycloud.base.codegen.model.po.TableEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器   工具类
 *
 * @author
 */
@Slf4j
public class GenUtils {

    private static final String ENTITY_JAVA_VM = "Entity.java.vm";
    private static final String MAPPER_JAVA_VM = "Mapper.java.vm";
    private static final String SERVICE_JAVA_VM = "Service.java.vm";
    private static final String SERVICE_IMPL_JAVA_VM = "ServiceImpl.java.vm";
    private static final String CONTROLLER_JAVA_VM = "Controller.java.vm";
    private static final String MAPPER_XML_VM = "Mapper.xml.vm";
    private static final String MENU_SQL_VM = "menu.sql.vm";
    private static final String INDEX_VUE_VM = "index.vue.vm";
    private static final String API_JS_VM = "api.js.vm";
    private static final String CRUD_JS_VM = "crud.js.vm";

    private static List<String> getTemplates(int dalType) {
        List<String> templates = new ArrayList<>();
        String dalValue = DALTypeEnum.acquire(dalType).getValue();

        templates.add("template/" + dalValue + "/Entity.java.vm");
        templates.add("template/" + dalValue + "/Mapper.java.vm");
        templates.add("template/" + dalValue + "/Mapper.xml.vm");
        templates.add("template/" + dalValue + "/Service.java.vm");
        templates.add("template/" + dalValue + "/ServiceImpl.java.vm");

        templates.add("template/Controller.java.vm");
//		templates.add("template/menu.sql.vm");

//		templates.add("template/index.vue.vm");
//		templates.add("template/api.js.vm");
//		templates.add("template/crud.js.vm");
        return templates;
    }

    /**
     * 生成代码
     */
    public static void generatorCode(Codegen genConfig, Map<String, String> table,
                                     List<Map<String, String>> columns, ZipOutputStream zip) {
        //配置信息
        Configuration config = getConfig();
        boolean hasBigDecimal = false;
        boolean hasDate = false;
        boolean hasDatetime = false;
        boolean hasTimestamp = false;
        boolean hasUtilDate = false;
        //表信息
        TableEntity tableEntity = new TableEntity();
        tableEntity.setTableName(table.get("tableName"));

        if (StrUtil.isNotBlank(genConfig.getRemark())) {
            tableEntity.setComments(genConfig.getRemark());
        } else {
            tableEntity.setComments(table.get("tableComment"));
        }

        String tablePrefix = genConfig.getTablePrefix();
        if (StrUtil.isBlank(genConfig.getTablePrefix())) {
            tablePrefix = config.getString("tablePrefix");
        }

        String entityNameSuffix = genConfig.getEntityNameSuffix();
        if (StrUtil.isBlank(entityNameSuffix)) {
            entityNameSuffix = config.getString("entityNameSuffix");
        }

        //表名转换成Java类名
        String className = tableToJava(tableEntity.getTableName(), tablePrefix);
        tableEntity.setCaseClassName(className);
        tableEntity.setEntityName(className + entityNameSuffix);
        tableEntity.setLowerClassName(StringUtils.uncapitalize(className));

        //列信息
        List<ColumnEntity> columnList = new ArrayList<>();
        for (Map<String, String> column : columns) {
            ColumnEntity columnEntity = new ColumnEntity();
            columnEntity.setColumnName(column.get("columnName"));
            columnEntity.setDataType(column.get("dataType"));
            columnEntity.setComments(column.get("columnComment"));
            columnEntity.setExtra(column.get("extra"));

            //列名转换成Java属性名
            String attrName = columnToJava(columnEntity.getColumnName());
            columnEntity.setCaseAttrName(attrName);
            columnEntity.setLowerAttrName(StringUtils.uncapitalize(attrName));

            //列的数据类型，转换成Java类型
            String attrType = config.getString(columnEntity.getDataType(), "unknowType");
            columnEntity.setAttrType(attrType);
            String entityAttrType = attrType.substring(attrType.lastIndexOf(".") + 1);
            columnEntity.setEntityAttrType(entityAttrType);
            if (!hasBigDecimal && "java.math.BigDecimal".equals(attrType)) {
                hasBigDecimal = true;
            }
            if (!hasDate && "java.sql.Date".equals(attrType)) {
                hasDate = true;
            }
//			if (!hasDatetime && "java.sql.Timestamp".equals(attrType)) {
//				hasDatetime = true;
//			}
            if (!hasTimestamp && "java.sql.Timestamp".equals(attrType)) {
                hasTimestamp = true;
            }

            if (!hasTimestamp && "java.util.Date".equals(attrType)) {
                hasUtilDate = true;
            }
            //是否主键
            if ("PRI".equalsIgnoreCase(column.get("columnKey")) && tableEntity.getPk() == null) {
                tableEntity.setPk(columnEntity);
            }

            columnList.add(columnEntity);
        }
        tableEntity.setColumns(columnList);

        //没主键，则第一个字段为主键
        if (tableEntity.getPk() == null) {
            tableEntity.setPk(tableEntity.getColumns().get(0));
        }

        //设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);
        //封装模板数据
        Map<String, Object> map = new HashMap<>(16);
        map.put("tableName", tableEntity.getTableName());
        map.put("pk", tableEntity.getPk());
        map.put("className", tableEntity.getCaseClassName());
        map.put("classname", tableEntity.getLowerClassName());
        map.put("pathName", tableEntity.getLowerClassName().toLowerCase());
        map.put("columns", tableEntity.getColumns());
        map.put("hasBigDecimal", hasBigDecimal);
        map.put("hasDate", hasDate);
        map.put("hasDatetime", hasDatetime);
        map.put("hasTimestamp", hasTimestamp);
        map.put("hasUtilDate", hasUtilDate);

        map.put("datetime", DateUtil.now());

        if (StrUtil.isNotBlank(genConfig.getRemark())) {
            map.put("comments", genConfig.getRemark());
        } else {
            map.put("comments", tableEntity.getComments());
        }

        if (StrUtil.isNotBlank(genConfig.getAuthor())) {
            map.put("author", genConfig.getAuthor());
        } else {
            map.put("author", config.getString("author"));
        }

        if (StrUtil.isNotBlank(genConfig.getModuleName())) {
            map.put("moduleName", genConfig.getModuleName());
        } else {
            map.put("moduleName", config.getString("moduleName"));
        }

        if (StrUtil.isNotBlank(genConfig.getPackageName())) {
            map.put("package", genConfig.getPackageName());
            map.put("mainPath", genConfig.getPackageName());
        } else {
            map.put("package", config.getString("package"));
            map.put("mainPath", config.getString("mainPath"));
        }
        VelocityContext context = new VelocityContext(map);

        //获取模板列表
        List<String> templates = getTemplates(genConfig.getDalType());
        for (String template : templates) {
            //渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, CharsetUtil.UTF_8);
            tpl.merge(context, sw);

            try {
                String aPackage = map.get("package").toString();
                String moduleName = map.get("moduleName").toString();
                String fileName = getFileName(template, tableEntity.getCaseClassName(), aPackage, moduleName);

                //添加到zip
                zip.putNextEntry(new ZipEntry(Objects.requireNonNull(fileName)));
                IoUtil.write(zip, CharsetUtil.UTF_8, false, sw.toString());
                IoUtil.close(sw);
                zip.closeEntry();
            } catch (IOException e) {
                throw new CheckedException("渲染模板失败，表名：" + tableEntity.getTableName(), e);
            }
        }
    }


    /**
     * 列名转换成Java属性名
     */
    private static String columnToJava(String columnName) {
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "");
    }

    /**
     * 表名转换成Java类名
     */
    private static String tableToJava(String tableName, String tablePrefix) {
        if (StringUtils.isNotBlank(tablePrefix)) {
            tableName = tableName.replace(tablePrefix, "");
        }
        return columnToJava(tableName);
    }

    /**
     * 获取配置信息
     */
    private static Configuration getConfig() {
        try {
            return new PropertiesConfiguration("generator.properties");
        } catch (ConfigurationException e) {
            throw new CheckedException("获取配置文件失败，", e);
        }
    }

    /**
     * 获取文件名
     */
    private static String getFileName(String template, String className, String packageName, String moduleName) {
        String packagePath = CommonConstants.BACK_END_PROJECT + File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator;
        if (StringUtils.isNotBlank(packageName)) {
            packagePath += packageName.replace(".", File.separator) + File.separator + moduleName + File.separator;
        }


        if (template.contains(ENTITY_JAVA_VM)) {
            return packagePath + "model" + File.separator + "po" + File.separator + className + ".java";
        }


        if (template.contains(MAPPER_JAVA_VM)) {
            return packagePath + "mapper" + File.separator + className + "Mapper.java";
        }

        if (template.contains(SERVICE_JAVA_VM)) {
            return packagePath + "service" + File.separator + className + "Service.java";
        }

        if (template.contains(SERVICE_IMPL_JAVA_VM)) {
            return packagePath + "service" + File.separator + "impl" + File.separator + className + "ServiceImpl.java";
        }
        if (template.contains(MAPPER_XML_VM)) {
            return CommonConstants.BACK_END_PROJECT + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "mapper" + File.separator + className + "Mapper.xml";
        }

        if (template.contains(CONTROLLER_JAVA_VM)) {
            return packagePath + "controller" + File.separator + className + "Controller.java";
        }


        if (template.contains(MENU_SQL_VM)) {
            return className.toLowerCase() + "_menu.sql";
        }

        if (template.contains(INDEX_VUE_VM)) {
            return CommonConstants.FRONT_END_PROJECT + File.separator + "src" + File.separator + "views" +
                    File.separator + moduleName + File.separator + className.toLowerCase() + File.separator + "index.vue";
        }

        if (template.contains(API_JS_VM)) {
            return CommonConstants.FRONT_END_PROJECT + File.separator + "src" + File.separator + "api" + File.separator + moduleName + File.separator + className.toLowerCase() + ".js";
        }

        if (template.contains(CRUD_JS_VM)) {
            return CommonConstants.FRONT_END_PROJECT + File.separator + "src" + File.separator + "const" +
                    File.separator + "crud" + File.separator + moduleName + File.separator + className.toLowerCase() + ".js";
        }

        return null;
    }
}
