export interface PartnerBrand {
  name: string;
  category: string;
}

export interface TestimonialItem {
  name: string;
  role: string;
  company: string;
  avatar: string;
  content: string;
  rating: number;
  tag: string;
  tagClass: string;
}

export interface FaqItem {
  question: string;
  answer: string;
}

export interface ContactFormModel {
  name: string;
  email: string;
  subject: string;
  message: string;
}
