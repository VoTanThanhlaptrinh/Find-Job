export class ApiResponse<T> {
  constructor(
    public message: string,
    public data: T,
    public status: number,
    public traceId?: string
  ) {}
}
