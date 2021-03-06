package com.wy.plugins;

import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.mybatis.generator.api.GeneratedJavaFile;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.DefaultJavaFormatter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.internal.util.StringUtility;

import com.wy.utils.StrUtils;

/**
 * controller生成器
 * 
 * @author paradiseWy
 */
public class CrlPlugin extends PluginAdapter {

	// 基础controller类
	private String baseCrl;
	// controller工程路径
	private String targetProjectMain;
	// controller层包名
	private String targetPackageCrl;
	// service 层包名
	private String targetPackageService;

	/**
	 * 取得配置文件中的值
	 */
	@Override
	public void setProperties(Properties properties) {
		super.setProperties(properties);

		String baseCrl = this.properties.getProperty("baseCrl");
		if (StringUtility.stringHasValue(baseCrl)) {
			this.baseCrl = baseCrl;
		}

		String targetProjectMain = this.properties.getProperty("targetProjectMain");
		if (StringUtility.stringHasValue(targetProjectMain)) {
			this.targetProjectMain = targetProjectMain;
		} else {
			throw new RuntimeException("controller 缺少必要的工程路径");
		}

		String targetPackageCrl = this.properties.getProperty("targetPackageCrl");
		if (StringUtility.stringHasValue(targetPackageCrl)) {
			this.targetPackageCrl = targetPackageCrl;
		} else {
			throw new RuntimeException("controller 缺少必要的包路径");
		}

		String targetPackageService = this.properties.getProperty("targetPackageService");
		if (StringUtility.stringHasValue(targetPackageService)) {
			this.targetPackageService = targetPackageService;
		} else {
			throw new RuntimeException("service 缺少必要的包路径");
		}
	}

	/**
	 * 验证该文件是否可用
	 */
	@Override
	public boolean validate(List<String> warnings) {
		return true;
	}

	/**
	 * 生成额外的class类,而不是生成service接口
	 */
	@Override
	public List<GeneratedJavaFile> contextGenerateAdditionalJavaFiles(IntrospectedTable introspectedTable) {
		List<GeneratedJavaFile> result = new ArrayList<GeneratedJavaFile>();
		// 获取实体类
		FullyQualifiedJavaType entityType = new FullyQualifiedJavaType(introspectedTable.getBaseRecordType());
		// 生成controller
		TopLevelClass crlClass = new TopLevelClass(new FullyQualifiedJavaType(
				MessageFormat.format("{0}.{1}Crl", targetPackageCrl, entityType.getShortName())));
		crlClass.setVisibility(JavaVisibility.PUBLIC);
		crlClass.addImportedType(entityType);
		crlClass.addImportedType(
				new FullyQualifiedJavaType("org.springframework.beans.factory.annotation.Autowired"));
		crlClass.addImportedType(
				new FullyQualifiedJavaType("org.springframework.web.bind.annotation.RequestMapping"));
		crlClass.addImportedType(
				new FullyQualifiedJavaType("org.springframework.web.bind.annotation.RestController"));
		// 添加注解
		crlClass.addAnnotation("@RestController");
		crlClass.addAnnotation(MessageFormat.format("{0}(\"{1}\")", "@RequestMapping",
				entityType.getShortName().toLowerCase()));
		// 基础crl类
		FullyQualifiedJavaType baseCrlClass = null;
		if (StrUtils.isNotBlank(baseCrl)) {
			baseCrlClass = new FullyQualifiedJavaType(baseCrl);
			baseCrlClass.addTypeArgument(entityType);
			crlClass.setSuperClass(baseCrlClass);
			FullyQualifiedJavaType serviceType = new FullyQualifiedJavaType(MessageFormat.format("{0}.{1}{2}",
					targetPackageService, entityType.getShortName(), "Service"));
			crlClass.addImportedType(serviceType);
			Field field = new Field(StrUtils.lowerFirst(serviceType.getShortName()), serviceType);
			field.addAnnotation("@Autowired");
			field.setVisibility(JavaVisibility.PRIVATE);
			crlClass.addField(field);
			// 重写抽象方法
			java.lang.reflect.Method[] ms;
			try {
				ms = Class.forName(baseCrl).getMethods();
				for (java.lang.reflect.Method m : ms) {
					Method method = new Method();
					if (Modifier.isAbstract(m.getModifiers())) {
						method.addAnnotation("@Override");
						method.setVisibility(JavaVisibility.PUBLIC);
						method.setReturnType(serviceType);
						method.setName(m.getName());
						method.addBodyLine("return " + StrUtils.lowerFirst(serviceType.getShortName()) + ";");
						crlClass.addMethod(method);
					}
				}
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		GeneratedJavaFile crlFile = new GeneratedJavaFile(crlClass, targetProjectMain,
				new DefaultJavaFormatter());
		result.add(crlFile);
		return result;
	}
}