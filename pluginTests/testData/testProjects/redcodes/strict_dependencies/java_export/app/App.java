public class App {
  private Exported exported;
  private InternalA internalA;
  private InternalB internalB;
  private StrictConsumer strictConsumer;
  private <error descr="Using type StrictDep from an indirect dependency @//lib:strict_dep">StrictDep</error> strictDep;
}
