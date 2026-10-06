import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, test, vi } from "vitest";
import CurrencyInput from "./CurrencyInput";

describe("CurrencyInput", () => {
  test("valor numérico vindo de fora é exibido formatado em pt-BR", () => {
    render(<CurrencyInput id="valor" value={1234.5} onChange={() => {}} />);
    const input = screen.getByRole("textbox");
    expect(input.value).toBe("1.234,50");
  });

  test("valor zero é exibido como 0,00, não como vazio", () => {
    render(<CurrencyInput id="valor" value={0} onChange={() => {}} />);
    const input = screen.getByRole("textbox");
    expect(input.value).toBe("0,00");
  });

  test("onChange recebe número em reais (não string, não centavos)", () => {
    const onChange = vi.fn();
    render(<CurrencyInput id="valor" value="" onChange={onChange} />);
    const input = screen.getByRole("textbox");

    fireEvent.change(input, { target: { value: "150" } });

    expect(onChange).toHaveBeenCalledWith(1.5);
  });

  test("apagar tudo volta a chamar onChange com string vazia (não com 0 ou NaN)", () => {
    const onChange = vi.fn();
    render(<CurrencyInput id="valor" value={10} onChange={onChange} />);
    const input = screen.getByRole("textbox");

    fireEvent.change(input, { target: { value: "" } });

    expect(onChange).toHaveBeenCalledWith("");
  });

  test("ignora caracteres não numéricos digitados (ex: letras, símbolos)", () => {
    const onChange = vi.fn();
    render(<CurrencyInput id="valor" value="" onChange={onChange} />);
    const input = screen.getByRole("textbox");

    fireEvent.change(input, { target: { value: "abc12xyz" } });

    // só os dígitos "12" devem valer => 0,12
    expect(onChange).toHaveBeenCalledWith(0.12);
  });
});
