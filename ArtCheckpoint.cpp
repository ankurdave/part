#include <iostream>

#include <string>
#include <algorithm>
#include <vector>
#include <chrono>
#include <cassert>
#include <random>
#include <cmath>

#include "art.cpp"

int m_0 = 10000000;
int key_len = 4;

unsigned char* gen_key() {
    unsigned char* str = new unsigned char[key_len];
    for (int j = 0; j < key_len; j++) {
        str[j] = static_cast<unsigned char>(rand() % 256);
    }
    return str;
}

class discreteZipf {
public:
    discreteZipf(double a, unsigned long imax)
        : max0(imax), alpha(a) {
        std::random_device rd;
        gen = std::mt19937(rd());
        urand = std::uniform_real_distribution<double>();
        init();
    }

    /// Return a discrete random number in the range of [0, imax].
    unsigned long operator()() {return next();}
    unsigned long next() {
	if (alpha > 1.0) { // rejection-inversion
	    while (true) {
		double ur = urand(gen);
		ur = hxm + ur * hx0;
		double x = Hinv(ur);
		unsigned long k = static_cast<unsigned long>(0.5+x);
		if (k - x <= ss)
		    return k;
		else if (ur >= H(0.5+k) - exp(-log(k+1.0)*alpha))
		    return k;
	    }
	}
	else { // simple rejection
	    unsigned long k = ((long) (urand(gen) * max0)) % max0;
	    double freq = pow((1.0+k), -alpha);
	    while (urand(gen) >= freq) {
		k = ((long) (urand(gen) * max0)) % max0;
		freq = pow((1.0+k), -alpha);
	    }
	    return k;
	}
    } // next

private:
    // private member variables
    std::uniform_real_distribution<double> urand;
    std::mt19937 gen;
    long unsigned max0;
    double alpha, alpha1, alphainv, hx0, hxm, ss;

    // private member function
    double H(double x) {return (exp(alpha1*log(1.0+x)) * alphainv);}
    double Hinv(double x) {return exp(alphainv*log(alpha1*x)) - 1.0;}
    void init() {
	// enforce the condition that alpha >= 0 and max0 > 1
	if (max0 <= 1)
	    max0 = 100;
	if (! (alpha >= 0.0))
	    alpha = 1.0;
	if (alpha > 1.0) {
	    // the rejection-inversion algorithm of W. Hormann and
	    // G. Derflinger
	    alpha1 = 1.0 - alpha;
	    alphainv = 1.0 / alpha1;
	    hxm = H(max0 + 0.5);
	    hx0 = H(0.5) - 1.0 - hxm;
	    ss = 1 - Hinv(H(1.5)-exp(-alpha*log(2.0)));
	}
	else { // use a simple rejection scheme
	    alpha1 = 0.0;
	    alphainv = 0.0;
	    hxm = 0.0;
	    hx0 = 0.0;
	    ss  = 0.0;
	}
    }
}; // Zipf distribution

unsigned char* gen_zipf_key(discreteZipf& z) {
    unsigned char* str = new unsigned char[key_len];
    for (int j = 0; j < key_len; j++) {
        str[j] = static_cast<unsigned char>(z.next());
    }
    return str;
}

int main()
{
    ArtTree<int> art;
    for (int i = 0; i < m_0; i++) {
        art.insert(gen_key(), key_len, 1);
    }
    std::cout << "Initial checkpoint: " << art.checkpoint() << "\n";
    std::cout << "Depth: " << art.max_depth() << "\n";

    std::cout << "Uniform\n";
    {
        for (int m = 1; m <= m_0 * 10; m *= 2) {
            auto art2 = art.snapshot();
            for (int i = 0; i < m; i++) {
                unsigned char* str = gen_key();
                art2.insert(str, key_len, 2);
            }
            std::cout << m << " " << art2.checkpoint() << " " << art2.node_size() << "\n";
        }
    }

    std::cout << "Zipf, s=2.0\n";
    {
        discreteZipf z(2.0, 255);
        for (int m = 1; m <= m_0 * 10; m *= 2) {
            auto art2 = art.snapshot();
            for (int i = 0; i < m; i++) {
                unsigned char* str = gen_zipf_key(z);
                art2.insert(str, key_len, 2);
            }
            std::cout << m << " " << art2.checkpoint() << " " << art2.node_size() << "\n";
        }
    }
}
