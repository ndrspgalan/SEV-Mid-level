package tests;
import application.ValerianEconomicSystemBootstrap;
public final class ProfessionalBehaviorProfileServiceTest {
    private ProfessionalBehaviorProfileServiceTest(){}
    public static void main(String[] args){
        var system=ValerianEconomicSystemBootstrap.createJuniorSystem();
        var rebuilt=system.getProfessionalBehaviorProfileService().rebuildProfiles();
        if(!rebuilt.isEmpty()) throw new AssertionError("fresh system has no projected events");
        if(system.getProfessionalBehaviorProfileService().count()!=0) throw new AssertionError("repository replacement");
        System.out.println("ProfessionalBehaviorProfileServiceTest: PASSED");
    }
}
