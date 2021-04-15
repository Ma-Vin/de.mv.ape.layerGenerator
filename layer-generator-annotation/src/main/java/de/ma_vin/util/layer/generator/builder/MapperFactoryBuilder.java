package de.ma_vin.util.layer.generator.builder;

import de.ma_vin.util.layer.generator.annotations.mapper.BaseAccessMapper;
import de.ma_vin.util.layer.generator.annotations.mapper.BaseTransportMapper;
import de.ma_vin.util.layer.generator.annotations.mapper.ExtendingAccessMapper;
import de.ma_vin.util.layer.generator.annotations.mapper.ExtendingTransportMapper;
import lombok.extern.log4j.Log4j2;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

@SupportedAnnotationTypes(
        "de.ma_vin.util.layer.generator.annotations.mapper.*")
@SupportedSourceVersion(SourceVersion.RELEASE_14)
@Log4j2
public class MapperFactoryBuilder extends AbstractFactoryBuilder {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<Class<?>, Set<TypeElement>> annotatedClasses = createAnnotatedClassesMap(annotations, roundEnv);

        try {
            Set<GenerateInformation> accessMapperClassesToGenerate = determineClasses(annotatedClasses, MapperType.ACCESS);
            Set<GenerateInformation> transportMapperClassesToGenerate = determineClasses(annotatedClasses, MapperType.TRANSPORT);
            log.info("{} access mapper class to add at object factory", accessMapperClassesToGenerate.size());
            log.info("{} transport mapper class to add at object factory", transportMapperClassesToGenerate.size());
            generateFactory(accessMapperClassesToGenerate, MapperType.ACCESS.getFactoryClassName());
            generateFactory(transportMapperClassesToGenerate, MapperType.TRANSPORT.getFactoryClassName());
        } catch (NoSuchElementException | IOException e) {
            return false;
        }
        return true;
    }

    @Override
    protected Map<Class<?>, Set<TypeElement>> createDefaultAnnotatedClassesMap() {
        Map<Class<?>, Set<TypeElement>> annotatedClasses = new HashMap<>();

        annotatedClasses.put(BaseAccessMapper.class, new HashSet<>());
        annotatedClasses.put(BaseTransportMapper.class, new HashSet<>());

        annotatedClasses.put(ExtendingAccessMapper.class, new HashSet<>());
        annotatedClasses.put(ExtendingTransportMapper.class, new HashSet<>());

        return annotatedClasses;
    }

    @Override
    protected Map<String, Class<?>> getNameToClassMap() {
        Map<String, Class<?>> nameToClass = new HashMap<>();

        nameToClass.put(BaseAccessMapper.class.getSimpleName(), BaseAccessMapper.class);
        nameToClass.put(BaseTransportMapper.class.getSimpleName(), BaseTransportMapper.class);

        nameToClass.put(ExtendingAccessMapper.class.getSimpleName(), ExtendingAccessMapper.class);
        nameToClass.put(ExtendingTransportMapper.class.getSimpleName(), ExtendingTransportMapper.class);

        return nameToClass;
    }

    /**
     * Determines the base and the extending classes which should be provided by factory
     *
     * @param annotatedClasses the map which contains the annotation and their set of annotated classes
     * @param mapperType       the type of mapper wish should be determined
     * @return A set of information for generating the given extending type
     */
    private Set<GenerateInformation> determineClasses(Map<Class<?>, Set<TypeElement>> annotatedClasses, MapperType mapperType) {
        Set<GenerateInformation> classesToGenerate =
                switch (mapperType) {
                    case ACCESS -> determineExtendingClasses(annotatedClasses, ExtendingAccessMapper.class);
                    case TRANSPORT -> determineExtendingClasses(annotatedClasses, ExtendingTransportMapper.class);
                };

        Set<GenerateInformation> baseClasses =
                switch (mapperType) {
                    case ACCESS -> determineBaseClasses(annotatedClasses, BaseAccessMapper.class);
                    case TRANSPORT -> determineBaseClasses(annotatedClasses, BaseTransportMapper.class);
                };

        return aggregateBaseAndExtendingInformation(classesToGenerate, baseClasses);
    }

    /**
     * Determines the set of information for generating for a given annotation of extending type
     *
     * @param annotatedClasses the map which contains the annotation and their set of annotated classes
     * @param extendingClass   The extending annotation
     * @param <A>              Class of the extending annotation
     * @return A set of information for generating the given extending type
     */
    private <A extends Annotation> Set<GenerateInformation> determineExtendingClasses(Map<Class<?>, Set<TypeElement>> annotatedClasses, Class<A> extendingClass) {
        return annotatedClasses.get(extendingClass).stream()
                .map(e -> {
                    GenerateInformation generateInformation = new GenerateInformation();
                    generateInformation.setClassName(e.getSimpleName().toString());
                    generateInformation.setPackageName(e.getQualifiedName().toString().substring(0, e.getQualifiedName().toString().lastIndexOf(".")));
                    Class<?> extendedClass;
                    if (ExtendingAccessMapper.class.equals(extendingClass)) {
                        extendedClass = e.getAnnotation(ExtendingAccessMapper.class).value();
                    } else if (ExtendingTransportMapper.class.equals(extendingClass)) {
                        extendedClass = e.getAnnotation(ExtendingTransportMapper.class).value();
                    } else {
                        return generateInformation;
                    }
                    generateInformation.setBaseClassName(extendedClass.getSimpleName());
                    generateInformation.setBasePackageName(extendedClass.getPackageName());
                    generateInformation.setModelPackage(null);
                    return generateInformation;
                }).collect(Collectors.toSet());
    }

    /**
     * Determines the set of information for generating for a given annotation of base type
     *
     * @param annotatedClasses the map which contains the annotation and their set of annotated classes
     * @param extendingClass   The base annotation
     * @param <A>              Class of the base annotation
     * @return A set of information for generating the given base type
     */
    private <A extends Annotation> Set<GenerateInformation> determineBaseClasses(Map<Class<?>, Set<TypeElement>> annotatedClasses, Class<A> extendingClass) {
        return annotatedClasses.get(extendingClass).stream()
                .map(e -> {
                    GenerateInformation generateInformation = new GenerateInformation();
                    generateInformation.setClassName(e.getSimpleName().toString());
                    generateInformation.setPackageName(e.getQualifiedName().toString().substring(0, e.getQualifiedName().toString().lastIndexOf(".")));
                    generateInformation.setBaseClassName(generateInformation.getClassName());
                    generateInformation.setBasePackageName(generateInformation.getPackageName());
                    if (BaseAccessMapper.class.equals(extendingClass)) {
                        generateInformation.setModelPackage(e.getAnnotation(BaseAccessMapper.class).value());
                    } else if (BaseTransportMapper.class.equals(extendingClass)) {
                        generateInformation.setModelPackage(e.getAnnotation(BaseTransportMapper.class).value());
                    }
                    return generateInformation;
                }).collect(Collectors.toSet());
    }
}
