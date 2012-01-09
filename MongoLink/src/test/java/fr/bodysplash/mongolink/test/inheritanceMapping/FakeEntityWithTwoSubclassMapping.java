package fr.bodysplash.mongolink.test.inheritanceMapping;

import fr.bodysplash.mongolink.domain.mapper.EntityMap;
import fr.bodysplash.mongolink.domain.mapper.SubclassMap;
import fr.bodysplash.mongolink.test.entity.FakeChildEntity;
import fr.bodysplash.mongolink.test.entity.FakeEntity;
import fr.bodysplash.mongolink.test.entity.OtherFakeChildEntity;


public class FakeEntityWithTwoSubclassMapping extends EntityMap<FakeEntity> {

    public FakeEntityWithTwoSubclassMapping() {
        super(FakeEntity.class);
    }

    @Override
    public void map() {
        id(element().getId());
        subclass(new SubclassMap<FakeChildEntity>(FakeChildEntity.class) {

            @Override
            protected void map() {

            }
        });
        subclass(new SubclassMap<OtherFakeChildEntity>(OtherFakeChildEntity.class) {
            @Override
            protected void map() {

            }
        });
    }

}
