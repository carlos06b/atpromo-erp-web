import { useState } from "react";

// Input mascarado de valor monetário: digita-se só números e o valor vai se
// formando da direita pra esquerda, tipo maquininha/calculadora financeira
// (ex: aperta 1 -> 0,01, aperta 2 -> 0,12, aperta 3 -> 1,23). Backspace some
// com o último dígito normalmente.
//
// `value` é sempre um número em reais (ex: 12.5) ou "" quando vazio.
// `onChange` recebe de volta um número em reais (ou "" se o campo ficar vazio).

function numberToDigits(value) {
  if (value === "" || value === null || value === undefined) return "";
  const numberValue = Number(value);
  if (Number.isNaN(numberValue)) return "";
  return String(Math.round(numberValue * 100));
}

function digitsToDisplay(digits) {
  if (!digits) return "";
  const numberValue = parseInt(digits, 10) / 100;
  return numberValue.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export default function CurrencyInput({
  id,
  value,
  onChange,
  className,
  placeholder = "0,00",
  disabled,
  required,
}) {
  const [digits, setDigits] = useState(() => numberToDigits(value));

  const externalDigits = numberToDigits(value);
  const externalNum = externalDigits === "" ? null : parseInt(externalDigits, 10);
  const currentNum = digits === "" ? null : parseInt(digits, 10);
  if (externalNum !== currentNum) {
    // O valor veio de fora (ex: abrindo o form pra editar) - resincroniza sem
    // atrapalhar o que o usuário já digitou no mesmo valor.
    setDigits(externalDigits);
  }

  function handleChange(e) {
    const rawDigits = e.target.value.replace(/\D/g, "").replace(/^0+(?=\d)/, "");
    setDigits(rawDigits);
    onChange(rawDigits === "" ? "" : parseInt(rawDigits, 10) / 100);
  }

  return (
    <input
      type="text"
      inputMode="numeric"
      id={id}
      disabled={disabled}
      required={required}
      value={digitsToDisplay(digits)}
      onChange={handleChange}
      placeholder={placeholder}
      className={className}
    />
  );
}
