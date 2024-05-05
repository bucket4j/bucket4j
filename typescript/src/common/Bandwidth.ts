export class Bandwidth {

  public static readonly UNDEFINED_ID:string? = null;
  public static readonly UNSPECIFIED_TIME_OF_FIRST_REFILL:bigint = BigInt(0);
  
  private readonly capacity:bigint
  private readonly initialTokens:bigint
  private readonly refillPeriodNanos:bigint
  private readonly refillTokens:bigint
  private readonly refillIntervally:boolean
  private readonly timeOfFirstRefillMillis:bigint
  private readonly useAdaptiveInitialTokens:boolean
  private readonly id:string

  constructor(capacity:bigint, refillPeriodNanos:bigint, refillTokens:bigint, initialTokens:bigint, refillIntervally:boolean,
    timeOfFirstRefillMillis:bigint, useAdaptiveInitialTokens:boolean, id:string) {
    this.capacity = capacity;
    this.initialTokens = initialTokens;
    this.refillPeriodNanos = refillPeriodNanos;
    this.refillTokens = refillTokens;
    this.refillIntervally = refillIntervally;
    this.timeOfFirstRefillMillis = timeOfFirstRefillMillis;
    this.useAdaptiveInitialTokens = useAdaptiveInitialTokens;
    this.id = id;
  }

  public isIntervallyAligned():boolean {
      return this.timeOfFirstRefillMillis != Bandwidth.UNSPECIFIED_TIME_OF_FIRST_REFILL;
  }

  public getTimeOfFirstRefillMillis():bigint {
      return this.timeOfFirstRefillMillis;
  }

  public getCapacity():bigint {
      return this.capacity;
  }

  public getInitialTokens():bigint {
      return this.initialTokens;
  }

  public getRefillPeriodNanos():bigint {
      return this.refillPeriodNanos;
  }

  public getRefillTokens():bigint {
      return this.refillTokens;
  }

  public isUseAdaptiveInitialTokens():boolean {
      return this.useAdaptiveInitialTokens;
  }

  public isRefillIntervally():boolean {
      return this.refillIntervally;
  }

  public isGready():boolean {
      return !this.refillIntervally;
  }

  public getId():string {
      return this.id;
  }
}