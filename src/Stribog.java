public class Stribog {
  private int length;
  private int[] iv;
  private int[] N;
  private int[] Sigma;

  public Stribog(int length) {
    switch (length) {
      case 512:
        initialize(512);
        break;
      case 256:
        initialize(256);
        break;
      default:
        initialize(0);
        System.err.println("Not found!");
    }
  }

  private void initialize(int length) {
    this.length = length;
    this.iv = new int[64];
    this.N = new int[64];
    this.Sigma = new int[64];

    for (int i = 0; i < 64; i++) {
      N[i] = 0x00;
      Sigma[i] = 0x00;
      iv[i] = (length == 512) ? 0x00 : 0x01;
    }
  }

  public String getHash(int[] message) {
    int[] m = new int[64];
    int[] h = new int[64];
    System.arraycopy(iv, 0, h, 0, 64);

    int[] M = new int[message.length];
    System.arraycopy(message, 0, M, 0, message.length);

    int l = message.length;
    while (l >= 64) {
      System.arraycopy(M, l - 64, m, 0, 64);
      h = gN(N, h, m);
      N = add(N, Data.bv512);
      Sigma = add(Sigma, m);
      l -= 64;
    }

    for (int i = 0; i < 63 - l; i++) {
      m[i] = 0;
    }
    m[63 - l] = 0x01;
    if (l > 0) {
      System.arraycopy(M, 0, m, 63 - l + 1, l);
    }

    h = gN(N, h, m);
    int[] bv = new int[64];
    bv[62] = (l * 8) >> 8;
    bv[63] = (l * 8) & 0xFF;
    N = add(N, bv);
    Sigma = add(Sigma, m);

    h = gN(Data.bv00, h, N);
    h = gN(Data.bv00, h, Sigma);

    String[] hashHex = new String[h.length];
    for (int i = 0; i < h.length; i++) {
      hashHex[i] = Integer.toHexString(h[i]);
    }

    return String.join("-", hashHex);
  }

  private int[] gN(int[] N, int[] h, int[] m) {
    int[] K = xor(h, N);

    K = S(K);
    K = P(K);
    K = L(K);

    int[] t = E(K, m);
    t = xor(t, h);

    return xor(t, m);
  }

  private int[] xor(int[] a, int[] b) {
    int[] result = new int[a.length];
    for (int i = 0; i < a.length; i++) {
      result[i] = a[i] ^ b[i];
    }
    return result;
  }

  private int[] LPS(int[] state) {
    return L(P(S(state)));
  }

  private int[] S(int[] state) {
    int[] result = new int[64];
    for (int i = 0; i < 64; i++) {
      result[i] = Data.SBox[state[i]];
    }
    return result;
  }

  private int[] P(int[] state) {
    int[] result = new int[64];
    for (int i = 0; i < 64; i++) {
      result[i] = state[Data.Tau[i]];
    }
    return result;
  }

  private int[] L(int[] state) {
    int[] result = new int[64];
    for (int i = 0; i < 8; i++) {
      int[] v = new int[8];
      for (int k = 0; k < 8; k++) {
        for (int j = 0; j < 8; j++) {
          if ((state[i * 8 + k] & (1 << (7 - j))) != 0) {
            v = xor(v, Data.A[k * 8 + j]);
          }
        }
      }
      System.arraycopy(v, 0, result, i * 8, 8);
    }
    return result;
  }

  private int[] E(int[] K, int[] m) {
    int[] result = xor(K, m);
    int[] Ki = new int[64];
    System.arraycopy(K, 0, Ki, 0, 64);
    for (int i = 0; i < 12; i++) {
      result = LPS(result);
      Ki = keySchedule(Ki, i);
      result = xor(result, Ki);
    }
    return result;
  }

  private int[] keySchedule(int[] k, int i) {
    return LPS(xor(k, Data.C[i]));
  }

  private int[] add(int[] a, int[] b) {
    int[] result = new int[a.length];
    int r = 0;
    for (int i = a.length - 1; i >= 0; i--) {
      result[i] = (a[i] + b[i] + r) & 0xFF;
      r = ((a[i] + b[i]) >> 8) & 0xFF;
    }
    return result;
  }
}
